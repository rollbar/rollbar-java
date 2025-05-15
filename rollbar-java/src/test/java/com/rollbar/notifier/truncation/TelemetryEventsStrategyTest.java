package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;
import com.rollbar.api.payload.data.body.Body;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class TelemetryEventsStrategyTest {

  private TestPayloadBuilder payloadBuilder;
  private final TelemetryEventsStrategy sut = new TelemetryEventsStrategy();
  private static final int MAX_EVENTS = 10;

  @Before
  public void setUp() {
    payloadBuilder = new TestPayloadBuilder();
  }

  @Test
  public void ifPayloadIsNullItShouldNotTruncate() {
    TruncationStrategy.TruncationResult<Payload> result = sut.truncate(null);

    verifyNoTruncation(result);
  }

  @Test
  public void ifDataIsNullItShouldNotTruncate() {
    Payload payload = new Payload.Builder(payloadBuilder.createTestPayload())
      .data(null)
      .build();

    TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);

    verifyNoTruncation(result);
  }

  @Test
  public void ifBodyIsNullItShouldNotTruncate() {
    Payload payload = payloadBuilder.createTestPayload((Body) null);

    TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);

    verifyNoTruncation(result);
  }

  @Test
  public void ifTelemetryEventsEqualOrLessThanMaximumItShouldTruncate() {
    List<TelemetryEvent> telemetryEvents = createTelemetryEvents(MAX_EVENTS);
    Payload payload = payloadBuilder.createTestPayloadSingleTraceWithTelemetryEvents(
      1,
      telemetryEvents
    );

    TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);

    verifyNoTruncation(result);
  }

  @Test
  public void ifTelemetryEventsAreAboveMaximumItShouldTruncate() {
    List<TelemetryEvent> telemetryEvents = createTelemetryEvents(MAX_EVENTS + 1);
    Payload payload = payloadBuilder.createTestPayloadSingleTraceWithTelemetryEvents(
        1,
        telemetryEvents
    );

    TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);

    assertTrue(result.wasTruncated);
    assertNotNull(result.value);
    assertNotEquals(payload, result.value);
  }

  private void verifyNoTruncation(TruncationStrategy.TruncationResult<Payload> result) {
    assertFalse(result.wasTruncated);
    assertNull(result.value);
  }

  private List<TelemetryEvent> createTelemetryEvents(int quantity) {
    ArrayList<TelemetryEvent> telemetryEvents = new ArrayList<>();
    for (int i = 0; i < quantity; i++) {
      telemetryEvents.add(creteTelemetryEvent());
    }
    return telemetryEvents;
  }

  private TelemetryEvent creteTelemetryEvent() {
    return new TelemetryEvent(
        TelemetryType.MANUAL,
        Level.DEBUG,
        1L,
        Source.CLIENT,
        new HashMap<>()
    );
  }
}
