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
 * Default implementation of {@link TelemetryEventTracker}.
 *
 * <p>This tracker records telemetry events in memory using a bounded, thread-safe
 * buffer. When the configured maximum capacity is reached, the oldest events
 * are discarded to make room for new ones.
 *
 * <p>Recorded events are returned and cleared when {@link #dump()} is called.
 */
public class RollbarTelemetryEventTracker implements TelemetryEventTracker {
  public static final int MAXIMUM_CAPACITY_FOR_TELEMETRY_EVENTS = 100;
  private final int maximumTelemetryData;
  private final Queue<TelemetryEvent> telemetryEvents = new ConcurrentLinkedQueue<>();
  private final Provider<Long> timestampProvider;
  private static final String LOG_KEY_MESSAGE = "message";
  private static final String NAVIGATION_KEY_FROM = "from";
  private static final String NAVIGATION_KEY_TO = "to";
  private static final String NETWORK_KEY_METHOD = "method";
  private static final String NETWORK_KEY_URL = "url";
  private static final String NETWORK_KEY_STATUS_CODE = "status_code";
  private static final int NO_CAPACITY = 0;

  /**
   * Construct a {@link RollbarTelemetryEventTracker}.
   *
   * @param timestampProvider    A Provider of timestamps for the events
   * @param maximumTelemetryData Maximum number of accumulated events (This value can be between 0
   *                             and 100, exceed any of these thresholds and the closest will be
   *                             taken)
   */
  public RollbarTelemetryEventTracker(
      Provider<Long> timestampProvider,
      int maximumTelemetryData
  ) {
    if (maximumTelemetryData < NO_CAPACITY) {
      this.maximumTelemetryData = NO_CAPACITY;
    } else {
      this.maximumTelemetryData =
          Math.min(maximumTelemetryData, MAXIMUM_CAPACITY_FOR_TELEMETRY_EVENTS);
    }
    this.timestampProvider = timestampProvider;
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
    if (doNotRecordEvents()) {
      return;
    }

    if (maxCapacityReached()) {
      removeOldestEvent();
    }
    telemetryEvents.add(telemetryEvent);
  }

  private boolean doNotRecordEvents() {
    return maximumTelemetryData == NO_CAPACITY;
  }

  private boolean maxCapacityReached() {
    return telemetryEvents.size() >= maximumTelemetryData;
  }

  private void removeOldestEvent() {
    telemetryEvents.poll();
  }

  private long getTimestamp() {
    return timestampProvider.provide();
  }
}
