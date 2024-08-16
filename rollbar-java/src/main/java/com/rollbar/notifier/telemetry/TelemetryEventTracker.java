package com.rollbar.notifier.telemetry;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;

import java.util.List;

public interface TelemetryEventTracker {

  /**
   * Dump all the events recorded
   */
  List<TelemetryEvent> dump();

  /**
   * Record log telemetry event. ({@link TelemetryType#LOG}).
   *
   * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param source the {@link Source} this event is recorded from (e.g. {@link Source#CLIENT}).
   * @param message the message sent for this event (e.g. "hello world").
   */
  void recordLogEventFor(Level level, Source source, String message);

  /**
   * Record manual telemetry event. ({@link TelemetryType#MANUAL}) .
   *
   * @param level   the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param source the {@link Source} this event is recorded from (e.g. {@link Source#CLIENT}).
   * @param message the message sent for this event (e.g. "hello world").
   */
  void recordManualEventFor(Level level, Source source, String message);

  /**
   * Record navigation telemetry event with from (origin) and to (destination).({@link TelemetryType#NAVIGATION}) .
   *
   * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param source the {@link Source} this event is recorded from (e.g. {@link Source#CLIENT}).
   * @param from  the starting point (e.g. "SettingView").
   * @param to    the destination point (e.g. "HomeView").
   */
  void recordNavigationEventFor(Level level, Source source, String from, String to);

  /**
   * Record network telemetry event with method, url, and status code.({@link TelemetryType#NETWORK}).
   *
   * @param level      the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param source the {@link Source} this event is recorded from (e.g. {@link Source#CLIENT}).
   * @param method     the verb used (e.g. "POST").
   * @param url        the api url (e.g. "<a href="http://rollbar.com/test/api">http://rollbar.com/test/api</a>").
   * @param statusCode the response status code (e.g. "404").
   */
  void recordNetworkEventFor(Level level, Source source, String method, String url, String statusCode);
}
