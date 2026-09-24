package com.rollbar.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class AgentTelemetryStoreTest {

  @BeforeEach
  @AfterEach
  public void reset() {
    AgentTelemetryStore.resetForTesting();
  }

  @Test
  public void recordNetworkEvent_capturesRequestAndPayloadFields() {
    AgentTelemetryStore.setClockForTesting(() -> 1_000_000L);

    AgentTelemetryStore.recordNetworkEvent("GET", "https://api.example.com/charge", "404");

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("network", event.get("type"));
    assertEquals("critical", event.get("level"));
    assertEquals("server", event.get("source"));
    assertEquals("1000000", event.get("timestamp_ms"));
    assertEquals("GET", event.get("method"));
    assertEquals("https://api.example.com/charge", event.get("url"));
    assertEquals("404", event.get("status_code"));
  }

  @Test
  public void recordErrorEvent_isRecordedAsManual() {
    AgentTelemetryStore.recordErrorEvent("Network error: connection refused");

    Map<String, String> event = AgentTelemetryStore.getAll().get(0);
    assertEquals("manual", event.get("type"));
    assertEquals("Network error: connection refused", event.get("message"));
  }

  @Test
  public void record_omitsNullFieldsRatherThanStoringNulls() {
    // The maps cross a classloader boundary and end up in a JSON payload; a null value there is a
    // "null" string in the report at best.
    AgentTelemetryStore.recordNetworkEvent("GET", null, "500");

    Map<String, String> event = AgentTelemetryStore.getAll().get(0);
    assertFalse(event.containsKey("url"));
    assertNull(event.get("url"));
  }

  @Test
  public void getAll_dropsOldestOnceCapacityIsReached() {
    for (int i = 0; i < AgentTelemetryStore.MAX_EVENTS + 5; i++) {
      AgentTelemetryStore.recordNetworkEvent("GET", "https://api.example.com/" + i, "500");
    }

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(AgentTelemetryStore.MAX_EVENTS, events.size());
    assertEquals("https://api.example.com/5", events.get(0).get("url"), "oldest must be dropped");
  }

  @Test
  public void getAll_returnsSnapshotsCallersCannotCorrupt() {
    AgentTelemetryStore.recordNetworkEvent("GET", "https://api.example.com/charge", "404");

    List<Map<String, String>> first = AgentTelemetryStore.getAll();
    assertThrows(UnsupportedOperationException.class, () -> first.get(0).put("url", "tampered"));
    first.clear();

    assertEquals(1, AgentTelemetryStore.getAll().size());
  }

  @Test
  public void getAll_keepsTheSignatureTheSdkLooksUpReflectively() throws Exception {
    // AgentTelemetryEventTracker resolves these methods by name and casts getAll's result, so a
    // change here breaks the SDK at runtime rather than at compile time. The element types are
    // part of the contract too: the maps cross a classloader boundary, so they may hold only
    // types both classloaders agree on.
    Method getAll = AgentTelemetryStore.class.getMethod("getAll", ClassLoader.class);
    Method register = AgentTelemetryStore.class.getMethod("registerApplication", ClassLoader.class);

    for (Method method : new Method[] {getAll, register}) {
      assertTrue(Modifier.isPublic(method.getModifiers()), method.getName());
      assertTrue(Modifier.isStatic(method.getModifiers()), method.getName());
    }
    assertEquals("java.util.List<java.util.Map<java.lang.String, java.lang.String>>",
        getAll.getGenericReturnType().getTypeName());
  }

  @Test
  public void getAll_doesNotShowOneApplicationTheEventsOfAnother() throws Exception {
    // Two WARs in one container: the agent is loaded once for both, so without partitioning the
    // hostnames and paths of one would appear in the other's Rollbar reports.
    try (URLClassLoader firstApp = application(); URLClassLoader secondApp = application()) {
      AgentTelemetryStore.registerApplication(firstApp);
      AgentTelemetryStore.registerApplication(secondApp);

      recordAs(firstApp, "https://first.internal/charge");
      recordAs(secondApp, "https://second.internal/refund");

      assertEquals(singletonList("https://first.internal/charge"), urlsSeenBy(firstApp));
      assertEquals(singletonList("https://second.internal/refund"), urlsSeenBy(secondApp));
    }
  }

  @Test
  public void getAll_includesEventsRecordedByLoadersNestedInTheApplication() throws Exception {
    // A JSP or plugin classloader inside the deployment is still the deployment.
    try (URLClassLoader app = application();
        URLClassLoader nested = new URLClassLoader(new URL[0], app)) {
      AgentTelemetryStore.registerApplication(app);

      recordAs(nested, "https://first.internal/charge");

      assertEquals(singletonList("https://first.internal/charge"), urlsSeenBy(app));
    }
  }

  @Test
  public void getAll_givesUnattributedEventsToTheOnlyApplication() throws Exception {
    // ForkJoinPool.commonPool workers carry the system classloader, so calls made there cannot be
    // pinned to a deployment. With a single application — a fat jar, a plain process — they are
    // unambiguous, and dropping them would silently lose telemetry.
    try (URLClassLoader app = application()) {
      AgentTelemetryStore.registerApplication(app);

      recordAs(ClassLoader.getSystemClassLoader(), "https://shared.internal/charge");

      assertEquals(singletonList("https://shared.internal/charge"), urlsSeenBy(app));
    }
  }

  @Test
  public void getAll_withholdsUnattributedEventsWhenApplicationsShareTheJvm() throws Exception {
    try (URLClassLoader firstApp = application(); URLClassLoader secondApp = application()) {
      AgentTelemetryStore.registerApplication(firstApp);
      AgentTelemetryStore.registerApplication(secondApp);

      recordAs(ClassLoader.getSystemClassLoader(), "https://shared.internal/charge");

      assertTrue(urlsSeenBy(firstApp).isEmpty(), "ambiguous events must not be handed out");
      assertTrue(urlsSeenBy(secondApp).isEmpty(), "ambiguous events must not be handed out");
    }
  }

  @Test
  public void getAll_capacityIsPerApplication() throws Exception {
    // A busy deployment must not evict a quiet one's events.
    try (URLClassLoader firstApp = application(); URLClassLoader secondApp = application()) {
      AgentTelemetryStore.registerApplication(firstApp);
      AgentTelemetryStore.registerApplication(secondApp);

      recordAs(secondApp, "https://second.internal/refund");
      for (int i = 0; i < AgentTelemetryStore.MAX_EVENTS + 5; i++) {
        recordAs(firstApp, "https://first.internal/" + i);
      }

      assertEquals(AgentTelemetryStore.MAX_EVENTS, urlsSeenBy(firstApp).size());
      assertEquals(singletonList("https://second.internal/refund"), urlsSeenBy(secondApp));
    }
  }

  /** A stand-in for a deployment's classloader: its own, parented to the system classloader. */
  private static URLClassLoader application() {
    return new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
  }

  /** Records an event the way an HTTP call made by {@code origin}'s code would. */
  private static void recordAs(ClassLoader origin, String url) {
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(origin);
    try {
      AgentTelemetryStore.recordNetworkEvent("GET", url, "500");
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

  private static List<String> urlsSeenBy(ClassLoader application) {
    List<String> urls = new ArrayList<>();
    for (Map<String, String> event : AgentTelemetryStore.getAll(application)) {
      urls.add(event.get("url"));
    }
    return urls;
  }
}
