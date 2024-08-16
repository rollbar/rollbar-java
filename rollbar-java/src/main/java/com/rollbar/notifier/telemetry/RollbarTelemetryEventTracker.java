package com.rollbar.notifier.telemetry;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;
import com.rollbar.notifier.provider.Provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Default {@link TelemetryEventTracker}.
 */
public class RollbarTelemetryEventTracker implements TelemetryEventTracker {
  private final int maximumTelemetryData;
  private final Queue<TelemetryEvent> telemetryEvents = new ConcurrentLinkedQueue<>();
  private final Provider<Long> timestampProvider;
  private static final String LOG_KEY_MESSAGE = "message";
  private static final String NAVIGATION_KEY_FROM = "from";
  private static final String NAVIGATION_KEY_TO = "to";
  private static final String NETWORK_KEY_METHOD = "method";
  private static final String NETWORK_KEY_URL = "url";
  private static final String NETWORK_KEY_STATUS_CODE = "status_code";

  /**
   * Construct a {@link RollbarTelemetryEventTracker}.
   *
   * @param timestampProvider    A Provider of timestamps for the events
   * @param maximumTelemetryData Maximum number of accumulated events
   */
  public RollbarTelemetryEventTracker(
      Provider<Long> timestampProvider,
      int maximumTelemetryData
  ) {
    this.timestampProvider = timestampProvider;
    this.maximumTelemetryData = maximumTelemetryData;
  }

  @Override
  public List<TelemetryEvent> dump() {
    List<TelemetryEvent> events = new ArrayList<>(telemetryEvents);
    telemetryEvents.clear();
    return events;
  }

  @Override
  public void recordLogEventFor(Level level, Source source, String message) {
    Map<String, String> body = new HashMap<>();
    body.put(LOG_KEY_MESSAGE, message);
    addEvent(new TelemetryEvent(TelemetryType.LOG, level, getTimestamp(), source, body));
  }

  @Override
  public void recordManualEventFor(Level level, Source source, String message) {
    Map<String, String> body = new HashMap<>();
    body.put(LOG_KEY_MESSAGE, message);
    addEvent(new TelemetryEvent(TelemetryType.MANUAL, level, getTimestamp(), source, body));
  }

  @Override
  public void recordNavigationEventFor(Level level, Source source, String from, String to) {
    Map<String, String> body = new HashMap<>();
    body.put(NAVIGATION_KEY_FROM, from);
    body.put(NAVIGATION_KEY_TO, to);
    addEvent(new TelemetryEvent(TelemetryType.NAVIGATION, level, getTimestamp(), source, body));
  }

  @Override
  public void recordNetworkEventFor(
      Level level,
      Source source,
      String method,
      String url,
      String statusCode
  ) {
    Map<String, String> body = new HashMap<>();
    body.put(NETWORK_KEY_METHOD, method);
    body.put(NETWORK_KEY_URL, url);
    body.put(NETWORK_KEY_STATUS_CODE, statusCode);
    addEvent(new TelemetryEvent(TelemetryType.NETWORK, level, getTimestamp(), source, body));
  }

  private void addEvent(TelemetryEvent telemetryEvent) {
    if (telemetryEvents.size() >= maximumTelemetryData) {
      telemetryEvents.poll();
    }
    telemetryEvents.add(telemetryEvent);
  }

  private long getTimestamp() {
    return timestampProvider.provide();
  }
}
