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

  public RollbarTelemetryEventTracker(
      Provider<Long> timestampProvider,
      int maximumTelemetryData
  ) {
    this.timestampProvider = timestampProvider;
    this.maximumTelemetryData = maximumTelemetryData;
  }

  public List<TelemetryEvent> dump() {
    List<TelemetryEvent> events = new ArrayList<>(telemetryEvents);
    telemetryEvents.clear();
    return events;
  }

  /**
   * Record log telemetry event. ({@link TelemetryType#LOG}).
   *
   * @param level   the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param message the message sent for this event (e.g. "hello world").
   */
  public void recordLogEventFor(Level level, Source source, String message) {
    Map<String, String> body = new HashMap<>();
    body.put(LOG_KEY_MESSAGE, message);
    addEvent(new TelemetryEvent(TelemetryType.LOG, level, timestampProvider.provide(), source, body));
  }

  /**
   * Record manual telemetry event. ({@link TelemetryType#MANUAL}) .
   *
   * @param level   the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param message the message sent for this event (e.g. "hello world").
   */
  public void recordManualEventFor(Level level, Source source, String message) {
    Map<String, String> body = new HashMap<>();
    body.put(LOG_KEY_MESSAGE, message);
    addEvent(new TelemetryEvent(TelemetryType.MANUAL, level, timestampProvider.provide(), source, body));
  }

  /**
   * Record navigation telemetry event with from (origin) and to (destination).({@link TelemetryType#NAVIGATION}) .
   *
   * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param from  the starting point (e.g. "SettingView").
   * @param to    the destination point (e.g. "HomeView").
   */
  public void recordNavigationEventFor(Level level, Source source, String from, String to) {
    Map<String, String> body = new HashMap<>();
    body.put(NAVIGATION_KEY_FROM, from);
    body.put(NAVIGATION_KEY_TO, to);
    addEvent(new TelemetryEvent(TelemetryType.NAVIGATION, level, timestampProvider.provide(), source, body));
  }

  /**
   * Record network telemetry event with method, url, and status code.({@link TelemetryType#NETWORK}).
   *
   * @param level      the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param method     the verb used (e.g. "POST").
   * @param url        the api url (e.g. "<a href="http://rollbar.com/test/api">http://rollbar.com/test/api</a>").
   * @param statusCode the response status code (e.g. "404").
   */
  public void recordNetworkEventFor(Level level, Source source, String method, String url, String statusCode) {
    Map<String, String> body = new HashMap<>();
    body.put(NETWORK_KEY_METHOD, method);
    body.put(NETWORK_KEY_URL, url);
    body.put(NETWORK_KEY_STATUS_CODE, statusCode);
    addEvent(new TelemetryEvent(TelemetryType.NETWORK, level, timestampProvider.provide(), source, body));
  }

  private void addEvent(TelemetryEvent telemetryEvent) {
    if (telemetryEvents.size() >= maximumTelemetryData) {
      telemetryEvents.poll();
    }
    telemetryEvents.add(telemetryEvent);
  }
}
