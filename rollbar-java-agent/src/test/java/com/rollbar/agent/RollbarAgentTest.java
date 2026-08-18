package com.rollbar.agent;

import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;


public class RollbarAgentTest {

  @BeforeEach
  public void setUp() {
    AgentTelemetryStore.initForTesting(System::currentTimeMillis);
  }

  @Test
  public void getTelemetryTracker_returnsSingletonInstance() {
    TelemetryEventTracker first = RollbarAgent.getTelemetryTracker();
    TelemetryEventTracker second = RollbarAgent.getTelemetryTracker();
    assertSame(first, second);
  }

  @Test
  public void init_withCustomTimestamp_usesProvidedTimestamp() {
    long fixedTime = 1_000_000L;
    AgentTelemetryStore.initForTesting(() -> fixedTime);

    AgentTelemetryStore.getInstance().recordManualEventFor(
        com.rollbar.api.payload.data.Level.WARNING,
        com.rollbar.api.payload.data.Source.CLIENT,
        "test"
    );

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    assertEquals(fixedTime, events.get(0).asJson().get("timestamp_ms"));
  }

  @Test
  public void errorReportingListener_reportsFailureToStderr() {
    String output = captureStderr(listener ->
        listener.onError("java.net.HttpURLConnection", null, null, false,
            new IllegalArgumentException("Unsupported class file major version 69")));

    assertTrue(output.contains("java.net.HttpURLConnection"), "must name the type that failed");
    assertTrue(output.contains("Unsupported class file major version 69"),
        "must surface the underlying cause");
  }

  @Test
  public void errorReportingListener_capsOutput() {
    // A ByteBuddy/JDK version mismatch fails for every instrumented type; an agent must not
    // flood the host process's stderr.
    int attempts = RollbarAgent.ErrorReportingListener.MAX_REPORTS + 20;
    String output = captureStderr(listener -> {
      for (int i = 0; i < attempts; i++) {
        listener.onError("com.example.Type" + i, null, null, false, new RuntimeException("boom"));
      }
    });

    assertTrue(output.contains("com.example.Type0"), "first failure must be reported");
    assertFalse(output.contains("com.example.Type" + (attempts - 1)),
        "reporting must stop once the cap is reached");
    assertTrue(output.contains("further instrumentation errors suppressed"),
        "must say that output was truncated");
  }

  private static String captureStderr(Consumer<RollbarAgent.ErrorReportingListener> action) {
    PrintStream original = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    try {
      System.setErr(new PrintStream(captured, true));
      action.accept(new RollbarAgent.ErrorReportingListener());
    } finally {
      System.setErr(original);
    }
    return captured.toString();
  }

  @Test
  public void urlSanitizer_stripsQueryAndFragment() {
    String sanitized = UrlSanitizer.sanitize("https://api.example.com/path?token=secret#section");
    assertEquals("https://api.example.com/path", sanitized);
  }

  @Test
  public void urlSanitizer_handlesNullGracefully() {
    assertNull(UrlSanitizer.sanitize(null));
  }

  @Test
  public void urlSanitizer_handlesInvalidUrl() {
    String raw = "not a url";
    assertEquals(raw, UrlSanitizer.sanitize(raw));
  }
}
