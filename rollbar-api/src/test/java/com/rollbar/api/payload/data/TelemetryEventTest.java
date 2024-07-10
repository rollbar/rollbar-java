package com.rollbar.api.payload.data;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.HashMap;

public class TelemetryEventTest {
  private static final Level LEVEL = Level.DEBUG;
  private static final String SOURCE = "Any message";
  private static final long TIMESTAMP = 10L;

  @Test
  public void shouldBeEqual() {
    TelemetryEvent telemetryEvent1 = logEvent();
    TelemetryEvent telemetryEvent2 = logEvent();

    assertThat(telemetryEvent1, is(telemetryEvent2));
  }

  private TelemetryEvent logEvent() {
    return new TelemetryEvent(TelemetryType.LOG, LEVEL, TIMESTAMP, SOURCE, new HashMap<>());
  }
}
