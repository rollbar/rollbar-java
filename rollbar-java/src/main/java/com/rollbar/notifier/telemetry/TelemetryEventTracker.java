package com.rollbar.notifier.telemetry;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;
import com.rollbar.notifier.provider.timestamp.TimestampProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TelemetryEventTracker {
  private final int maximumTelemetryData;
  private final Queue<TelemetryEvent> telemetryEvents = new ConcurrentLinkedQueue<>();
  private final TimestampProvider timestampProvider;
  private static final String LOG_KEY_MESSAGE = "message";
  private static final String NAVIGATION_KEY_FROM = "from";
  private static final String NAVIGATION_KEY_TO = "to";
  private static final String NETWORK_KEY_METHOD = "method";
  private static final String NETWORK_KEY_URL = "url";
  private static final String NETWORK_KEY_STATUS_CODE = "status_code";

  public TelemetryEventTracker(
      TimestampProvider timestampProvider,
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
  public void recordLogEventFor(Level level, String source, final String message) {
    Map<String, String> body = new HashMap<String, String>() {
      private static final long serialVersionUID = 3746979871039874692L;

      {
        put(LOG_KEY_MESSAGE, message);
      }
    };
    addEvent(new TelemetryEvent(TelemetryType.LOG, level, timestampProvider.provide(), source, body));
  }

  /**
   * Record manual telemetry event. ({@link TelemetryType#MANUAL}) .
   *
   * @param level   the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param message the message sent for this event (e.g. "hello world").
   */
  public void recordManualEventFor(Level level, String source, final String message) {
    Map<String, String> body = new HashMap<String, String>() {
      private static final long serialVersionUID = 3746979871039874692L;

      {
        put(LOG_KEY_MESSAGE, message);
      }
    };
    addEvent(new TelemetryEvent(TelemetryType.MANUAL, level, timestampProvider.provide(), source, body));
  }

  /**
   * Record navigation telemetry event with from (origin) and to (destination).({@link TelemetryType#NAVIGATION}) .
   *
   * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param from  the starting point (e.g. "SettingView").
   * @param to    the destination point (e.g. "HomeView").
   */
  public void recordNavigationEventFor(Level level, String source, final String from, final String to) {
    Map<String, String> body = new HashMap<String, String>() {
      private static final long serialVersionUID = 3746979871039874692L;

      {
        put(NAVIGATION_KEY_FROM, from);
        put(NAVIGATION_KEY_TO, to);
      }
    };
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
  public void recordNetworkEventFor(Level level, String source, final String method, final String url, final String statusCode) {
    Map<String, String> body = new HashMap<String, String>() {
      private static final long serialVersionUID = 3746979871039874692L;

      {
        put(NETWORK_KEY_METHOD, method);
        put(NETWORK_KEY_URL, url);
        put(NETWORK_KEY_STATUS_CODE, statusCode);
      }
    };
    addEvent(new TelemetryEvent(TelemetryType.NETWORK, level, timestampProvider.provide(), source, body));
  }

  private void addEvent(TelemetryEvent telemetryEvent) {
    if (telemetryEvents.size() >= maximumTelemetryData) {
      telemetryEvents.poll();
    }
    telemetryEvents.add(telemetryEvent);
  }
}
