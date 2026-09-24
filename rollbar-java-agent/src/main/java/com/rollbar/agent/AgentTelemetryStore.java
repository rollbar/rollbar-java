package com.rollbar.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded, in-memory buffer of the telemetry events the agent's instrumentation records.
 *
 * <p><strong>Nothing in this package may reference a {@code com.rollbar.api} or
 * {@code com.rollbar.notifier} type.</strong> The agent jar is appended to the <em>system</em>
 * class path by {@code -javaagent:}, while the Rollbar SDK usually lives in a child classloader —
 * {@code BOOT-INF/lib} under a Spring Boot fat jar, {@code WEB-INF/lib} under a servlet container.
 * Parent delegation only looks upward, so an SDK type named from here resolves against the system
 * classloader and is not found. In a method signature of the {@code Premain-Class} that is fatal
 * before the application starts: the JVM calls {@code getDeclaredMethods()} to locate
 * {@code premain}, which loads every type in every declared signature, and a
 * {@code NoClassDefFoundError} there aborts the JVM with "processing of -javaagent failed".
 *
 * <p>So events are held as plain {@link String} maps, which every classloader agrees on.
 * {@code com.rollbar.notifier.telemetry.AgentTelemetryEventTracker}, which ships in
 * {@code rollbar-java} and therefore loads in the application's own classloader, reads them back
 * through the system classloader and converts them into {@code TelemetryEvent}s.
 *
 * <p>Each map carries the reserved keys {@link #KEY_TYPE}, {@link #KEY_LEVEL}, {@link #KEY_SOURCE}
 * and {@link #KEY_TIMESTAMP_MS}; every other entry is the event body.
 */
public final class AgentTelemetryStore {

  /**
   * Telemetry type, as the Rollbar payload spells it ({@code network}, {@code manual}).
   */
  public static final String KEY_TYPE = "type";

  /**
   * Severity, as the Rollbar payload spells it ({@code critical}).
   */
  public static final String KEY_LEVEL = "level";

  /**
   * Event source, as the Rollbar payload spells it ({@code server}).
   */
  public static final String KEY_SOURCE = "source";

  /**
   * Event time in milliseconds since the epoch, as a decimal string.
   */
  public static final String KEY_TIMESTAMP_MS = "timestamp_ms";

  /**
   * Maximum number of buffered events; the oldest are dropped once it is reached.
   */
  public static final int MAX_EVENTS = 100;

  private static final String TYPE_NETWORK = "network";
  private static final String TYPE_MANUAL = "manual";
  private static final String LEVEL_CRITICAL = "critical";
  private static final String SOURCE_SERVER = "server";

  private static final String BODY_KEY_METHOD = "method";
  private static final String BODY_KEY_URL = "url";
  private static final String BODY_KEY_STATUS_CODE = "status_code";
  private static final String BODY_KEY_MESSAGE = "message";

  private static final Deque<Map<String, String>> EVENTS = new ArrayDeque<>();

  // Overridable so tests can assert on timestamps. Not a java.util.function type: keeping this
  // class free of anything but the most basic JDK types is what lets any classloader read it.
  private static volatile Clock clock = new SystemClock();

  private AgentTelemetryStore() {}

  /**
   * Records a network telemetry event for a request that returned a 4xx or 5xx status.
   *
   * @param method the HTTP verb (e.g. {@code GET}).
   * @param url the sanitized request URL.
   * @param statusCode the response status code, as a string.
   */
  public static void recordNetworkEvent(String method, String url, String statusCode) {
    Map<String, String> event = newEvent(TYPE_NETWORK);
    putIfPresent(event, BODY_KEY_METHOD, method);
    putIfPresent(event, BODY_KEY_URL, url);
    putIfPresent(event, BODY_KEY_STATUS_CODE, statusCode);
    add(event);
  }

  /**
   * Records a manual telemetry event for a request that failed before a response arrived.
   *
   * @param message the failure description.
   */
  public static void recordErrorEvent(String message) {
    Map<String, String> event = newEvent(TYPE_MANUAL);
    putIfPresent(event, BODY_KEY_MESSAGE, message);
    add(event);
  }

  /**
   * Returns a snapshot of the buffered events, oldest first.
   *
   * <p>This is the method the SDK-side tracker invokes reflectively, so its signature is part of
   * the agent's contract: it must stay {@code public static}, take no arguments, and return only
   * JDK types.
   *
   * @return the recorded events, each an independent copy.
   */
  public static List<Map<String, String>> getAll() {
    synchronized (EVENTS) {
      List<Map<String, String>> snapshot = new ArrayList<>(EVENTS.size());
      for (Map<String, String> event : EVENTS) {
        snapshot.add(Collections.unmodifiableMap(new HashMap<>(event)));
      }
      return snapshot;
    }
  }

  /**
   * Drops every buffered event. For tests.
   */
  public static void resetForTesting() {
    synchronized (EVENTS) {
      EVENTS.clear();
    }
    clock = new SystemClock();
  }

  /**
   * Replaces the clock used to timestamp events, so tests can assert on exact values. For tests.
   *
   * @param clock the replacement clock.
   */
  static void setClockForTesting(Clock clock) {
    AgentTelemetryStore.clock = clock;
  }

  private static Map<String, String> newEvent(String type) {
    Map<String, String> event = new HashMap<>();
    event.put(KEY_TYPE, type);
    event.put(KEY_LEVEL, LEVEL_CRITICAL);
    event.put(KEY_SOURCE, SOURCE_SERVER);
    event.put(KEY_TIMESTAMP_MS, Long.toString(clock.currentTimeMillis()));
    return event;
  }

  private static void putIfPresent(Map<String, String> event, String key, String value) {
    if (value != null) {
      event.put(key, value);
    }
  }

  private static void add(Map<String, String> event) {
    synchronized (EVENTS) {
      if (EVENTS.size() >= MAX_EVENTS) {
        EVENTS.pollFirst();
      }
      EVENTS.addLast(event);
    }
  }

  /**
   * Source of event timestamps.
   */
  interface Clock {
    long currentTimeMillis();
  }

  private static final class SystemClock implements Clock {
    @Override
    public long currentTimeMillis() {
      return System.currentTimeMillis();
    }
  }
}
