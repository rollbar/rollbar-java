package com.rollbar.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Bounded, in-memory buffer of the telemetry events the agent's instrumentation records, kept one
 * buffer per application.
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
 *
 * <p><strong>Partitioning.</strong> One JVM can host several applications — WARs in Tomcat or
 * WildFly — each with its own SDK, its own access token and its own Rollbar project. The agent is
 * loaded once for all of them, so a single shared buffer would put one application's hostnames,
 * paths and status codes into another's error reports, and let a busy application evict a quiet
 * one's events. Instead each event is filed under the context classloader of the thread that made
 * the HTTP call, which in a servlet container is the deployment's own classloader, and
 * {@link #getAll(ClassLoader)} returns only what the asking application is entitled to see: its
 * own events, and those recorded by classloaders nested inside it.
 *
 * <p>Events recorded by a classloader <em>above</em> the application — a thread whose context
 * classloader is the container's, such as a {@code ForkJoinPool.commonPool} worker — cannot be
 * attributed to any deployment. They are handed to the application only while it is the single
 * registered one in the JVM, which is the ordinary case of a fat jar or a standalone process.
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
   * Maximum number of buffered events <em>per application</em>; the oldest are dropped once it is
   * reached.
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

  private static final Object LOCK = new Object();

  // Weak keys: an undeployed application's classloader must stay collectable, and it would not be
  // if the store held it. The buffered values are Strings only, so nothing here points back at a
  // key and keeps its entry alive.
  private static final Map<ClassLoader, Deque<Map<String, String>>> EVENTS = new WeakHashMap<>();

  private static final Set<ClassLoader> APPLICATIONS =
      Collections.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());

  private static final Comparator<Map<String, String>> BY_TIMESTAMP =
      new Comparator<Map<String, String>>() {
        @Override
        public int compare(Map<String, String> left, Map<String, String> right) {
          return Long.compare(timestampOf(left), timestampOf(right));
        }
      };

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
   * Announces an application that will read events, so the store can tell how many share this JVM.
   *
   * <p>The SDK-side tracker calls this from its constructor, which runs at {@code Rollbar.init}:
   * every deployment has registered long before anything is reported, so whether an event can be
   * attributed to one application does not depend on who reports an error first.
   *
   * <p>Part of the reflective contract with {@code rollbar-java} — {@code public static}, one
   * {@link ClassLoader} argument.
   *
   * @param application the classloader of the application that will read events; {@code null} is
   *                    read as the system classloader.
   */
  public static void registerApplication(ClassLoader application) {
    synchronized (LOCK) {
      APPLICATIONS.add(orSystem(application));
    }
  }

  /**
   * Returns a snapshot of the events the given application may see, oldest first.
   *
   * <p>Part of the reflective contract with {@code rollbar-java} — {@code public static}, one
   * {@link ClassLoader} argument, returning only JDK types.
   *
   * @param application the classloader of the application asking, normally the one that loaded the
   *                    SDK; {@code null} is read as the system classloader.
   * @return the events visible to it, each an independent copy.
   */
  public static List<Map<String, String>> getAll(ClassLoader application) {
    ClassLoader requester = orSystem(application);
    List<Map<String, String>> snapshot = new ArrayList<>();

    synchronized (LOCK) {
      APPLICATIONS.add(requester);
      boolean unattributedAreMine = APPLICATIONS.size() == 1;
      for (Map.Entry<ClassLoader, Deque<Map<String, String>>> buffer : EVENTS.entrySet()) {
        if (!visibleTo(buffer.getKey(), requester, unattributedAreMine)) {
          continue;
        }
        for (Map<String, String> event : buffer.getValue()) {
          snapshot.add(Collections.unmodifiableMap(new HashMap<>(event)));
        }
      }
    }

    // Several buffers can contribute, and they interleave in time.
    snapshot.sort(BY_TIMESTAMP);
    return snapshot;
  }

  /**
   * Returns the events visible to the calling thread's context classloader.
   *
   * <p>For diagnostics and tests. An application reads its own events through
   * {@link #getAll(ClassLoader)}, which does not depend on which thread happens to ask.
   *
   * @return the events visible to the caller, each an independent copy.
   */
  public static List<Map<String, String>> getAll() {
    return getAll(contextClassLoader());
  }

  /**
   * Drops every buffered event and every registered application. For tests.
   */
  public static void resetForTesting() {
    synchronized (LOCK) {
      EVENTS.clear();
      APPLICATIONS.clear();
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

  /**
   * Whether events recorded under {@code origin} belong to the application {@code requester}.
   *
   * <p>Downward is safe: a classloader nested inside the application — a JSP or plugin loader — is
   * still that application. Upward is not: the container's classloader is shared by every
   * deployment, so events recorded there are attributed to no one unless there is only one
   * application to attribute them to. Sideways is another deployment, and never visible.
   */
  private static boolean visibleTo(ClassLoader origin, ClassLoader requester,
      boolean unattributedAreMine) {
    if (origin == requester || isNestedIn(origin, requester)) {
      return true;
    }
    if (isNestedIn(requester, origin)) {
      return unattributedAreMine;
    }
    return false;
  }

  /**
   * Whether {@code loader} is a strict descendant of {@code ancestor} in the delegation chain.
   */
  private static boolean isNestedIn(ClassLoader loader, ClassLoader ancestor) {
    for (ClassLoader parent = loader.getParent(); parent != null; parent = parent.getParent()) {
      if (parent == ancestor) {
        return true;
      }
    }
    return false;
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
    ClassLoader origin = contextClassLoader();
    synchronized (LOCK) {
      Deque<Map<String, String>> buffer = EVENTS.get(origin);
      if (buffer == null) {
        buffer = new ArrayDeque<>();
        EVENTS.put(origin, buffer);
      }
      if (buffer.size() >= MAX_EVENTS) {
        buffer.pollFirst();
      }
      buffer.addLast(event);
    }
  }

  // The thread that made the HTTP call is the only cheap evidence of which application made it: a
  // servlet container sets the context classloader to the deployment's own before handing it a
  // request, and a thread pool an application creates inherits it.
  private static ClassLoader contextClassLoader() {
    return orSystem(Thread.currentThread().getContextClassLoader());
  }

  private static ClassLoader orSystem(ClassLoader classLoader) {
    return classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
  }

  private static long timestampOf(Map<String, String> event) {
    try {
      return Long.parseLong(event.get(KEY_TIMESTAMP_MS));
    } catch (RuntimeException e) {
      return 0L;
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
