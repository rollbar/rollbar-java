package com.rollbar.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
  public void getAll_returnsOnlyJdkTypes() {
    // The SDK reads these maps through the system classloader, so every object in them has to be
    // a type both classloaders agree on.
    AgentTelemetryStore.recordNetworkEvent("GET", "https://api.example.com/charge", "404");

    for (Map<String, String> event : AgentTelemetryStore.getAll()) {
      for (Map.Entry<String, String> entry : event.entrySet()) {
        assertTrue(entry.getKey().getClass().getName().startsWith("java."));
        assertTrue(entry.getValue().getClass().getName().startsWith("java."));
      }
    }
  }
}
