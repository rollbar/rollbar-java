package com.rollbar.agent;

import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class RollbarAgentTest {

  @BeforeEach
  public void setUp() {
    AgentTelemetryStore.init(System::currentTimeMillis);
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
    AgentTelemetryStore.init(() -> fixedTime);

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
