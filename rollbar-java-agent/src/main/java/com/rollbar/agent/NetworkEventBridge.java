package com.rollbar.agent;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Called by JDK-class advice via reflection to bridge the classloader gap.
 *
 * <p>ByteBuddy advice inlined into bootstrap/platform classloader classes (e.g.
 * {@code HttpURLConnection}, {@code HttpClient}) cannot directly reference application-classloader
 * classes. Advice code uses {@code Thread.currentThread().getContextClassLoader().loadClass(...)}
 * to reach this class and delegates all Rollbar-specific logic here.
 */
public final class NetworkEventBridge {

  // Tracks connections/responses already recorded to deduplicate re-entrant calls.
  // WeakHashMap so entries are garbage-collected when the connection is released.
  private static final Set<Object> RECORDED = Collections.newSetFromMap(
      Collections.synchronizedMap(new WeakHashMap<>())
  );

  private NetworkEventBridge() {}

  public static void resetRecordedForTesting() {
    RECORDED.clear();
  }

  /**
   * Marks the given key as recorded. Returns {@code true} if this is the first time,
   * {@code false} if already recorded (duplicate/re-entrant call).
   */
  public static boolean markAsRecorded(Object key) {
    return RECORDED.add(key);
  }

  /**
   * Records a network telemetry event for the given key if not already recorded.
   *
   * <p>Uses the key as a deduplication token — subsequent calls with the same key are ignored.
   */
  public static void recordNetworkEvent(Object key, String method, String url, String statusCode) {
    if (!markAsRecorded(key)) {
      return; // deduplicate re-entrant calls for the same connection
    }
    AgentTelemetryStore.getInstance().recordNetworkEventFor(
        Level.CRITICAL,
        Source.SERVER,
        method,
        UrlSanitizer.sanitize(url),
        statusCode
    );
  }

  /**
   * Records a manual error telemetry event with the given message.
   *
   * <p>Called when an HTTP request fails with an I/O exception rather than a status code.
   */
  public static void recordError(String message) {
    AgentTelemetryStore.getInstance().recordManualEventFor(
        Level.CRITICAL,
        Source.SERVER,
        "Network error: " + (message != null ? message : "unknown")
    );
  }
}
