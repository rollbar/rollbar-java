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

  // Re-entry guard for the getInputStream/getErrorStream advice: invoking getResponseCode() to
  // trigger recording can, on connection-level failures (responseCode stays -1), cause the JDK's
  // getResponseCode() to call getInputStream() again — re-firing the advice and recursing until a
  // StackOverflowError. This ThreadLocal breaks that loop.
  private static final ThreadLocal<Boolean> TRIGGERING_RESPONSE_CODE =
      ThreadLocal.withInitial(() -> Boolean.FALSE);

  private NetworkEventBridge() {}

  public static void resetRecordedForTesting() {
    RECORDED.clear();
    TRIGGERING_RESPONSE_CODE.remove();
  }

  /**
   * Returns {@code true} if the caller may proceed to trigger {@code getResponseCode()};
   * {@code false} if a trigger is already in progress on this thread (re-entrant call).
   *
   * <p>The caller that receives {@code true} must call {@link #exitResponseCodeTrigger()} in a
   * {@code finally} block. A re-entrant caller receives {@code false} and must not call exit.
   */
  public static boolean enterResponseCodeTrigger() {
    if (TRIGGERING_RESPONSE_CODE.get()) {
      return false;
    }
    TRIGGERING_RESPONSE_CODE.set(Boolean.TRUE);
    return true;
  }

  /**
   * Clears the re-entry guard set by {@link #enterResponseCodeTrigger()}.
   */
  public static void exitResponseCodeTrigger() {
    TRIGGERING_RESPONSE_CODE.remove();
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
   * Returns a {@link java.util.function.BiConsumer} that records telemetry when an async
   * HTTP response completes. Intended to be chained via {@code CompletableFuture.whenComplete}.
   *
   * <p>The callback is created here (in the app classloader) so it can reference Rollbar types
   * directly, avoiding the reflection overhead that advice code needs to cross the classloader gap.
   */
  public static java.util.function.BiConsumer<Object, Throwable> createAsyncCallback(
      Object request) {
    return (response, thrown) -> {
      try {
        if (thrown != null) {
          if (markAsRecorded(thrown)) {
            String message = thrown.getMessage() != null
                ? thrown.getMessage() : thrown.getClass().getName();
            recordError(message);
          }
          return;
        }
        if (response != null) {
          // Look up methods via public interfaces, not the internal JDK implementation class.
          // NetworkEventBridge runs in the app classloader (unnamed module) and cannot access
          // jdk.internal.net.http.*; java.net.http.* is exported and accessible.
          Class<?> httpResponseIface = Class.forName("java.net.http.HttpResponse");
          Class<?> httpRequestIface = Class.forName("java.net.http.HttpRequest");
          int statusCode = (Integer) httpResponseIface.getMethod("statusCode").invoke(response);
          if (statusCode >= 400) {
            Object uri = httpRequestIface.getMethod("uri").invoke(request);
            String method = (String) httpRequestIface.getMethod("method").invoke(request);
            recordNetworkEvent(response, method, uri.toString(), String.valueOf(statusCode));
          }
        }
      } catch (Throwable ignored) {
        // Callback must never throw — swallow all errors
      }
    };
  }

  /**
   * Joins a base URI with a request URI, for clients that dispatch a target host separately from a
   * request whose URI may be relative.
   *
   * <p>Apache HC's {@code doExecute(HttpHost, request, context)} receives the target host as its
   * own argument, so a request issued through the host-based {@code execute(HttpHost, request)}
   * overloads carries only a path (e.g. {@code /charge}). Rejoining the two is what keeps the host
   * in the recorded URL. A request URI that is already absolute is returned untouched, and a null
   * base (HC leaves the target null for a relative URI it could not resolve) degrades to the path
   * alone.
   *
   * @param baseUri the target host as a URI (e.g. {@code https://api.example.com}), or null
   * @param requestUri the request URI, absolute or relative, or null
   * @return the joined URL — never null, so the caller always has something to sanitize
   */
  public static String composeUrl(String baseUri, String requestUri) {
    if (requestUri == null || requestUri.isEmpty()) {
      return baseUri != null ? baseUri : "";
    }
    if (isAbsolute(requestUri)) {
      return requestUri;
    }
    if (baseUri == null) {
      return requestUri;
    }
    if (requestUri.startsWith("/")) {
      return baseUri.concat(requestUri);
    }
    return baseUri.concat("/").concat(requestUri);
  }

  // Bound the "://" search to the characters before the first '/', '?', or '#', i.e. to where a
  // scheme could legally appear. A relative request URI can carry a nested absolute URL in its
  // query or path — /api/redirect?url=https://other.example.com/foo from an OAuth redirect
  // endpoint, URL shortener, or proxy-style API — and treating that as already-absolute would drop
  // the target host, leaving the sanitized telemetry URL as a bare path with no host at all.
  private static boolean isAbsolute(String requestUri) {
    for (int i = 0; i < requestUri.length(); i++) {
      char character = requestUri.charAt(i);
      if (character == '/' || character == '?' || character == '#') {
        return false;
      }
      if (character == ':' && requestUri.startsWith("://", i)) {
        return true;
      }
    }
    return false;
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
