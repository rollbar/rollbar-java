package com.rollbar.agent;

import com.rollbar.notifier.telemetry.TelemetryEventTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RollbarAgentTest {

  @BeforeEach
  public void setUp() {
    AgentTelemetryStore.resetForTesting();
  }

  @Test
  public void getTelemetryTracker_returnsSingletonInstance() {
    TelemetryEventTracker first = RollbarAgent.getTelemetryTracker();
    TelemetryEventTracker second = RollbarAgent.getTelemetryTracker();
    assertSame(first, second);
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
