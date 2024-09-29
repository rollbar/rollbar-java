package com.rollbar.notifier.telemetry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;
import com.rollbar.notifier.provider.timestamp.TimestampProvider;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RollbarTelemetryEventTrackerTest {

  private static final Source SOURCE = Source.SERVER;
  private static final String MESSAGE = "Any message";
  private static final String FROM = "Any origin";
  private static final String TO = "Any destination";
  private static final String METHOD = "Any method";
  private static final String URL = "Any url";
  private static final String STATUS_CODE = "Any status code";
  private static final Level LEVEL = Level.DEBUG;
  private static final int MAXIMUM_TELEMETRY_DATA = 2;
  private static final long TIMESTAMP = 10L;
  private final TimestampProvider fakeTimestampProvider = new TimestampProviderFake();
  private final TelemetryEventTracker telemetryEventTracker = newEventTracker(MAXIMUM_TELEMETRY_DATA);
  private static final int MINIMUM_CAPACITY_FOR_TELEMETRY_EVENTS = 1;
  private static final int MAXIMUM_CAPACITY_FOR_TELEMETRY_EVENTS = 50;

  @Test
  public void shouldDiscardOldestEventsWhenMaxCapacityIsReached() {
    telemetryEventTracker.recordManualEventFor(LEVEL, SOURCE, MESSAGE);
    telemetryEventTracker.recordLogEventFor(LEVEL, SOURCE, MESSAGE);
    telemetryEventTracker.recordLogEventFor(LEVEL, SOURCE, MESSAGE);

    List<TelemetryEvent> telemetryEvents = telemetryEventTracker.dump();

    assertThat(telemetryEvents.size(), is(MAXIMUM_TELEMETRY_DATA));
    verifyContainsOnlyLogEvents(telemetryEvents);
  }

  @Test
  public void shouldTrackALogEvent() {
    Map<String, Object> expectedJson = getExpectedJsonForALogTelemetryEvent();

    telemetryEventTracker.recordLogEventFor(LEVEL, SOURCE, MESSAGE);

    assertThat(getTrackedEventAsJson(), is(expectedJson));
  }

  @Test
  public void shouldTrackAManualEvent() {
    Map<String, Object> expectedJson = getExpectedJsonForAManualTelemetryEvent();

    telemetryEventTracker.recordManualEventFor(LEVEL, SOURCE, MESSAGE);

    assertThat(getTrackedEventAsJson(), is(expectedJson));
  }

  @Test
  public void shouldTrackANavigationEvent() {
    Map<String, Object> expectedJson = getExpectedJsonForANavigationTelemetryEvent();

    telemetryEventTracker.recordNavigationEventFor(LEVEL, SOURCE, FROM, TO);

    assertThat(getTrackedEventAsJson(), is(expectedJson));
  }

  @Test
  public void shouldTrackANetworkEvent() {
    Map<String, Object> expectedJson = getExpectedJsonForANetworkTelemetryEvent();

    telemetryEventTracker.recordNetworkEventFor(LEVEL, SOURCE, METHOD, URL, STATUS_CODE);

    assertThat(getTrackedEventAsJson(), is(expectedJson));
  }

  @Test
  public void shouldSetTheMaximumTelemetryDataLimitedToItsLowerLimit() {
    TelemetryEventTracker telemetryEventTracker = newEventTracker(MINIMUM_CAPACITY_FOR_TELEMETRY_EVENTS - 1);

    List<TelemetryEvent> telemetryEvents = record70EventsAndDump(telemetryEventTracker);

    assertThat(telemetryEvents.size(), is(MINIMUM_CAPACITY_FOR_TELEMETRY_EVENTS));
  }

  @Test
  public void shouldSetTheMaximumTelemetryDataLimitedToItsUpperLimit() {
    TelemetryEventTracker telemetryEventTracker = newEventTracker(MAXIMUM_CAPACITY_FOR_TELEMETRY_EVENTS + 1);

    List<TelemetryEvent> telemetryEvents = record70EventsAndDump(telemetryEventTracker);

    assertThat(telemetryEvents.size(), is(MAXIMUM_CAPACITY_FOR_TELEMETRY_EVENTS));
  }

  @Test
  public void shouldSetTheMaximumTelemetryDataLimitedToAValueBetweenBounds() {
    int maximumTelemetryEvents = 20;
    TelemetryEventTracker telemetryEventTracker = newEventTracker(maximumTelemetryEvents);

    List<TelemetryEvent> telemetryEvents = record70EventsAndDump(telemetryEventTracker);

    assertThat(telemetryEvents.size(), is(maximumTelemetryEvents));
  }

  private TelemetryEventTracker newEventTracker(int maximumTelemetryData) {
    return new RollbarTelemetryEventTracker(
        fakeTimestampProvider,
        maximumTelemetryData
    );
  }

  private List<TelemetryEvent> record70EventsAndDump(TelemetryEventTracker telemetryEventTracker) {
    for (int i = 0; i < 70; i++) {
      telemetryEventTracker.recordManualEventFor(LEVEL, SOURCE, MESSAGE);
    }
    return telemetryEventTracker.dump();
  }

  private Map<String, Object> getTrackedEventAsJson() {
    return getFirstEvent().asJson();
  }

  private TelemetryEvent getFirstEvent() {
    return telemetryEventTracker.dump().get(0);
  }

  private Map<String, Object> getExpectedJsonForALogTelemetryEvent() {
    Map<String, Object> map = commonFields();
    map.put("type", TelemetryType.LOG.asJson());

    Map<String, String> body = new HashMap<>();
    body.put("message", MESSAGE);

    map.put("body", body);
    return map;
  }

  private Map<String, Object> getExpectedJsonForAManualTelemetryEvent() {
    Map<String, Object> map = commonFields();
    map.put("type", TelemetryType.MANUAL.asJson());

    Map<String, String> body = new HashMap<>();
    body.put("message", MESSAGE);

    map.put("body", body);
    return map;
  }

  private Map<String, Object> getExpectedJsonForANavigationTelemetryEvent() {
    Map<String, Object> map = commonFields();
    map.put("type", TelemetryType.NAVIGATION.asJson());

    Map<String, String> body = new HashMap<>();
    body.put("from", FROM);
    body.put("to", TO);

    map.put("body", body);
    return map;
  }

  private Map<String, Object> getExpectedJsonForANetworkTelemetryEvent() {
    Map<String, Object> map = commonFields();
    map.put("type", TelemetryType.NETWORK.asJson());

    Map<String, String> body = new HashMap<>();
    body.put("method", METHOD);
    body.put("url", URL);
    body.put("status_code", STATUS_CODE);

    map.put("body", body);
    return map;
  }

  private Map<String, Object> commonFields() {
    Map<String, Object> map = new HashMap<>();
    map.put("level", LEVEL.asJson());
    map.put("source", SOURCE.asJson());
    map.put("timestamp_ms", TIMESTAMP);
    return map;
  }

  private void verifyContainsOnlyLogEvents(List<TelemetryEvent> telemetryEvents) {
    Map<String, Object> expectedJson = getExpectedJsonForALogTelemetryEvent();

    for (int index = 0; index < telemetryEvents.size(); index++) {
      TelemetryEvent telemetryEvent = telemetryEvents.get(index);
      assertThat(telemetryEvent.asJson(), is(expectedJson));
    }
  }

  private static class TimestampProviderFake extends TimestampProvider {
    @Override
    public Long provide() {
      return TIMESTAMP;
    }
  }
}
