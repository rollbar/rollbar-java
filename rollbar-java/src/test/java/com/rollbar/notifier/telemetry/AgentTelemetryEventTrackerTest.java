package com.rollbar.notifier.telemetry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentTelemetryEventTrackerTest {

  private static final int MAXIMUM_TELEMETRY_DATA = 4;

  private final FakeAgentEventSource agentEvents = new FakeAgentEventSource();
  private final RollbarTelemetryEventTracker delegate =
      new RollbarTelemetryEventTracker(new FixedTimestampProvider(), MAXIMUM_TELEMETRY_DATA);
  private final AgentTelemetryEventTracker sut =
      new AgentTelemetryEventTracker(delegate, agentEvents, MAXIMUM_TELEMETRY_DATA);

  @Test
  public void shouldConvertAgentEventsIntoTelemetryEvents() {
    agentEvents.add(networkEvent(10L, "404"));

    List<TelemetryEvent> events = sut.getAll();

    Map<String, String> body = new HashMap<>();
    body.put("method", "GET");
    body.put("url", "https://api.example.com/charge");
    body.put("status_code", "404");
    assertThat(events, is(Collections.singletonList(new TelemetryEvent(
        TelemetryType.NETWORK, Level.CRITICAL, 10L, Source.SERVER, body))));
  }

  @Test
  public void shouldMergeAgentAndApplicationEventsInTimestampOrder() {
    agentEvents.add(networkEvent(10L, "500"));
    agentEvents.add(networkEvent(30L, "503"));
    // The delegate timestamps its own events; the fake clock puts this one between the two above.
    sut.recordManualEventFor(Level.INFO, Source.SERVER, "in between");

    List<TelemetryEvent> events = sut.getAll();

    assertThat(events.size(), is(3));
    assertThat(timestampOf(events.get(0)), is(10L));
    assertThat(timestampOf(events.get(1)), is(FixedTimestampProvider.TIMESTAMP));
    assertThat(timestampOf(events.get(2)), is(30L));
  }

  @Test
  public void shouldKeepTheMostRecentEventsWhenOverCapacity() {
    for (int i = 0; i < MAXIMUM_TELEMETRY_DATA + 3; i++) {
      agentEvents.add(networkEvent(i, String.valueOf(500 + i)));
    }

    List<TelemetryEvent> events = sut.getAll();

    assertThat(events.size(), is(MAXIMUM_TELEMETRY_DATA));
    assertThat(timestampOf(events.get(0)), is(3L));
  }

  @Test
  public void shouldIgnoreUnreadableAgentEvents() {
    // The maps come from a separately versioned artifact across a classloader boundary, so a
    // malformed one must not take the whole telemetry timeline with it.
    Map<String, String> unknownType = networkEvent(10L, "500");
    unknownType.put("type", "something-new");
    agentEvents.add(unknownType);
    agentEvents.add(networkEvent(20L, "502"));

    List<TelemetryEvent> events = sut.getAll();

    assertThat(events.size(), is(1));
    assertThat(timestampOf(events.get(0)), is(20L));
  }

  @Test
  public void shouldBehaveAsTheDefaultTrackerWhenTheAgentIsNotAttached() {
    sut.recordLogEventFor(Level.DEBUG, Source.SERVER, "a message");

    assertThat(sut.getAll(), is(delegate.getAll()));
  }

  private static long timestampOf(TelemetryEvent event) {
    return (Long) event.asJson().get("timestamp_ms");
  }

  private static Map<String, String> networkEvent(long timestamp, String statusCode) {
    Map<String, String> event = new HashMap<>();
    event.put("type", "network");
    event.put("level", "critical");
    event.put("source", "server");
    event.put("timestamp_ms", String.valueOf(timestamp));
    event.put("method", "GET");
    event.put("url", "https://api.example.com/charge");
    event.put("status_code", statusCode);
    return event;
  }

  private static class FakeAgentEventSource implements AgentTelemetryEventTracker.AgentEventSource {

    private final List<Map<String, String>> events = new ArrayList<>();

    void add(Map<String, String> event) {
      events.add(event);
    }

    @Override
    public List<Map<String, String>> getAll() {
      return new ArrayList<>(events);
    }
  }

  private static class FixedTimestampProvider
      implements com.rollbar.notifier.provider.Provider<Long> {

    static final long TIMESTAMP = 20L;

    @Override
    public Long provide() {
      return TIMESTAMP;
    }
  }
}
