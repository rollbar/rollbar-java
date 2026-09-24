package com.rollbar.notifier.telemetry;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.provider.timestamp.TimestampProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TelemetryEventTracker} that merges the HTTP events recorded by {@code
 * rollbar-java-agent} with the events the application records itself.
 *
 * <p>Pass one to the config builder to make agent telemetry show up in reports:
 * <pre>
 *   Rollbar.init(withAccessToken("...")
 *       .telemetryEventTracker(new AgentTelemetryEventTracker())
 *       .build());
 * </pre>
 *
 * <p>Without the agent attached this behaves exactly like {@link RollbarTelemetryEventTracker}:
 * the agent's buffer is simply not there, which is logged once and then ignored.
 *
 * <p><strong>Why the events arrive as maps.</strong> {@code -javaagent:} appends the agent jar to
 * the <em>system</em> class path, so the agent's classes load in the system classloader. The SDK
 * normally does not: under a Spring Boot fat jar it lives in {@code BOOT-INF/lib}, under a servlet
 * container in {@code WEB-INF/lib}, both loaded by a child classloader the system classloader
 * cannot see. The agent therefore holds its events as plain {@link String} maps and never names an
 * SDK type, and this class — which loads in the application's classloader, where the SDK types
 * are — reads them back through the system classloader and converts them here.
 */
public class AgentTelemetryEventTracker implements TelemetryEventTracker {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AgentTelemetryEventTracker.class);

  private static final String AGENT_STORE_CLASS = "com.rollbar.agent.AgentTelemetryStore";
  private static final String AGENT_STORE_GET_ALL_METHOD = "getAll";

  private static final String KEY_TYPE = "type";
  private static final String KEY_LEVEL = "level";
  private static final String KEY_SOURCE = "source";
  private static final String KEY_TIMESTAMP_MS = "timestamp_ms";

  private static final Set<String> RESERVED_KEYS = reservedKeys();

  private final TelemetryEventTracker delegate;
  private final AgentEventSource agentEventSource;
  private final int maximumTelemetryData;

  /**
   * Construct an {@link AgentTelemetryEventTracker} holding up to
   * {@link RollbarTelemetryEventTracker#MAXIMUM_CAPACITY_FOR_TELEMETRY_EVENTS} events.
   */
  public AgentTelemetryEventTracker() {
    this(new TimestampProvider(),
        RollbarTelemetryEventTracker.MAXIMUM_CAPACITY_FOR_TELEMETRY_EVENTS);
  }

  /**
   * Construct an {@link AgentTelemetryEventTracker}.
   *
   * @param timestampProvider    A Provider of timestamps for the events this tracker records
   *                             itself. Events coming from the agent are timestamped by the agent.
   * @param maximumTelemetryData Maximum number of events returned by {@link #getAll()}, counting
   *                             the agent's and the application's together.
   */
  public AgentTelemetryEventTracker(Provider<Long> timestampProvider, int maximumTelemetryData) {
    this(new RollbarTelemetryEventTracker(timestampProvider, maximumTelemetryData),
        new SystemClassLoaderAgentEventSource(), maximumTelemetryData);
  }

  AgentTelemetryEventTracker(TelemetryEventTracker delegate, AgentEventSource agentEventSource,
      int maximumTelemetryData) {
    this.delegate = delegate;
    this.agentEventSource = agentEventSource;
    this.maximumTelemetryData = maximumTelemetryData;
  }

  /**
   * Get the application's and the agent's events, oldest first, capped at the configured maximum.
   */
  @Override
  public List<TelemetryEvent> getAll() {
    List<TelemetryEvent> agentEvents = readAgentEvents();
    List<TelemetryEvent> events = delegate.getAll();
    if (agentEvents.isEmpty()) {
      return events;
    }

    List<TimestampedEvent> merged = new ArrayList<>(events.size() + agentEvents.size());
    // The two buffers are each in order, but interleave in time, so the merged timeline has to be
    // sorted. The sort is stable, which keeps same-millisecond events in their recorded order.
    for (TelemetryEvent event : events) {
      merged.add(new TimestampedEvent(event));
    }
    for (TelemetryEvent event : agentEvents) {
      merged.add(new TimestampedEvent(event));
    }
    merged.sort(TimestampedEvent.BY_TIMESTAMP);

    int from = Math.max(0, merged.size() - Math.max(maximumTelemetryData, 0));
    List<TelemetryEvent> result = new ArrayList<>(merged.size() - from);
    for (TimestampedEvent timestamped : merged.subList(from, merged.size())) {
      result.add(timestamped.event);
    }
    return result;
  }

  @Override
  public void recordLogEventFor(Level level, Source source, String message) {
    delegate.recordLogEventFor(level, source, message);
  }

  @Override
  public void recordManualEventFor(Level level, Source source, String message) {
    delegate.recordManualEventFor(level, source, message);
  }

  @Override
  public void recordNavigationEventFor(Level level, Source source, String from, String to) {
    delegate.recordNavigationEventFor(level, source, from, to);
  }

  @Override
  public void recordNetworkEventFor(Level level, Source source, String method, String url,
      String statusCode) {
    delegate.recordNetworkEventFor(level, source, method, url, statusCode);
  }

  private List<TelemetryEvent> readAgentEvents() {
    List<Map<String, String>> raw = agentEventSource.getAll();
    if (raw == null || raw.isEmpty()) {
      return Collections.emptyList();
    }
    List<TelemetryEvent> events = new ArrayList<>(raw.size());
    for (Map<String, String> event : raw) {
      TelemetryEvent converted = toTelemetryEvent(event);
      if (converted != null) {
        events.add(converted);
      }
    }
    return events;
  }

  // The maps cross a classloader boundary and are written by a separately versioned artifact, so
  // nothing about their contents is guaranteed at compile time: an unreadable event is dropped
  // rather than allowed to break the payload it was meant to annotate.
  private static TelemetryEvent toTelemetryEvent(Map<String, String> event) {
    if (event == null) {
      return null;
    }
    TelemetryType type = telemetryTypeOf(event.get(KEY_TYPE));
    Level level = Level.lookupByName(event.get(KEY_LEVEL));
    if (type == null || level == null) {
      LOGGER.debug("Ignoring agent telemetry event with unknown type or level: {}", event);
      return null;
    }

    Map<String, String> body = new HashMap<>(event);
    body.keySet().removeAll(RESERVED_KEYS);

    return new TelemetryEvent(type, level, timestampOf(event), sourceOf(event.get(KEY_SOURCE)),
        body);
  }

  private static TelemetryType telemetryTypeOf(String name) {
    for (TelemetryType type : TelemetryType.values()) {
      if (type.asJson().equals(name)) {
        return type;
      }
    }
    return null;
  }

  private static Source sourceOf(String name) {
    for (Source source : Source.values()) {
      if (source.asJson().equals(name)) {
        return source;
      }
    }
    return Source.SERVER;
  }

  private static Long timestampOf(Map<String, String> event) {
    String timestamp = event.get(KEY_TIMESTAMP_MS);
    try {
      return Long.valueOf(timestamp);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static Set<String> reservedKeys() {
    Set<String> keys = new HashSet<>();
    keys.add(KEY_TYPE);
    keys.add(KEY_LEVEL);
    keys.add(KEY_SOURCE);
    keys.add(KEY_TIMESTAMP_MS);
    return Collections.unmodifiableSet(keys);
  }

  /** The agent's event buffer, as seen from the application's classloader. */
  interface AgentEventSource {
    List<Map<String, String>> getAll();
  }

  /**
   * Reads the agent's buffer reflectively through the system classloader.
   *
   * <p>The lookup goes through {@link ClassLoader#getSystemClassLoader()} rather than this class's
   * own loader: {@code -javaagent:} puts the agent there, and a child loader holding the SDK can
   * always reach up to it, while the reverse never works.
   *
   * <p>The call carries this class's own classloader, which is the application's: one agent serves
   * every application in the JVM, and that is what tells the store whose events to hand back. So
   * keep the SDK inside the application — {@code WEB-INF/lib}, not the container's shared
   * {@code lib} — or every deployment answers to the same classloader and to the same events.
   */
  private static final class SystemClassLoaderAgentEventSource implements AgentEventSource {

    private static final ClassLoader APPLICATION =
        AgentTelemetryEventTracker.class.getClassLoader();

    private volatile Method getAll;
    private volatile boolean lookupFailed;

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAll() {
      Method method = getAll;
      if (method == null && !lookupFailed) {
        method = resolve(AGENT_STORE_GET_ALL_METHOD);
        getAll = method;
      }
      if (method == null) {
        return Collections.emptyList();
      }
      try {
        return (List<Map<String, String>>) method.invoke(null, APPLICATION);
      } catch (Exception e) {
        LOGGER.warn("Could not read telemetry events from the Rollbar Java agent", e);
        return Collections.emptyList();
      }
    }

    private Method resolve(String name) {
      if (lookupFailed) {
        return null;
      }
      try {
        Class<?> store = ClassLoader.getSystemClassLoader().loadClass(AGENT_STORE_CLASS);
        return store.getMethod(name, ClassLoader.class);
      } catch (ClassNotFoundException e) {
        LOGGER.info("The Rollbar Java agent is not attached to this JVM; only telemetry events "
            + "recorded by the application will be reported. Add -javaagent:<rollbar-java-agent "
            + "jar> to capture HTTP errors automatically.");
      } catch (Exception e) {
        LOGGER.warn("The Rollbar Java agent is attached but its telemetry store could not be "
            + "read; check that the agent and rollbar-java versions match", e);
      }
      lookupFailed = true;
      return null;
    }
  }

  private static final class TimestampedEvent {

    static final Comparator<TimestampedEvent> BY_TIMESTAMP = new Comparator<TimestampedEvent>() {
      @Override
      public int compare(TimestampedEvent left, TimestampedEvent right) {
        return Long.compare(left.timestamp, right.timestamp);
      }
    };

    final TelemetryEvent event;
    final long timestamp;

    TimestampedEvent(TelemetryEvent event) {
      this.event = event;
      Object timestamp = event.asJson().get(KEY_TIMESTAMP_MS);
      this.timestamp = timestamp instanceof Number ? ((Number) timestamp).longValue() : 0L;
    }
  }
}
