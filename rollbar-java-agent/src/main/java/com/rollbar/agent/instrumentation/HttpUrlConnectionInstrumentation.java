package com.rollbar.agent.instrumentation;

import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Installs ByteBuddy advice on {@code java.net.HttpURLConnection} to capture network errors.
 */
public final class HttpUrlConnectionInstrumentation {

  private HttpUrlConnectionInstrumentation() {}

  /**
   * Instruments {@code HttpURLConnection} to record 4xx/5xx responses and network errors.
   *
   * <p>Three entry points are covered:
   * <ul>
   *   <li>{@code getResponseCode()} on the base class — catches callers that check the code
   *       explicitly.</li>
   *   <li>{@code getInputStream()} on concrete subclasses — catches the common pattern where the
   *       caller reads the body directly and only sees the IOException on 4xx.</li>
   *   <li>{@code getErrorStream()} on concrete subclasses — catches callers that check for an error
   *       stream after {@code connect()} or after catching the IOException from
   *       {@code getInputStream()}.</li>
   * </ul>
   *
   * <p>{@code getInputStream} and {@code getErrorStream} advice simply invoke
   * {@code getResponseCode()} to trigger the base-class advice; deduplication in
   * {@link com.rollbar.agent.NetworkEventBridge} ensures only one event is emitted per connection.
   */
  public static void install(AgentBuilder builder, Instrumentation inst) {
    builder
        .type(ElementMatchers.named("java.net.HttpURLConnection"))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(GetResponseCodeAdvice.class)
                .on(ElementMatchers.named("getResponseCode")))
        )
        .installOn(inst);

    // getInputStream() and getErrorStream() are overridden in concrete subclasses, so we must
    // target subtypes rather than java.net.HttpURLConnection itself.
    builder
        .type(ElementMatchers.isSubTypeOf(java.net.HttpURLConnection.class)
            .and(ElementMatchers.not(ElementMatchers.named("java.net.HttpURLConnection"))))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(GetInputStreamAdvice.class)
                .on(ElementMatchers.named("getInputStream")
                    .and(ElementMatchers.not(ElementMatchers.isAbstract()))))
             .visit(Advice.to(GetErrorStreamAdvice.class)
                .on(ElementMatchers.named("getErrorStream")
                    .and(ElementMatchers.not(ElementMatchers.isAbstract()))))
        )
        .installOn(inst);
  }

  /**
   * Advice inlined into concrete {@code HttpURLConnection.getInputStream()}.
   *
   * <p>When {@code getInputStream()} throws (4xx/5xx response), invokes {@code getResponseCode()}
   * so that {@link GetResponseCodeAdvice} records the event. Deduplication in
   * {@link com.rollbar.agent.NetworkEventBridge} prevents double-recording if the caller also calls
   * {@code getResponseCode()} or {@code getErrorStream()} afterwards.
   */
  public static class GetInputStreamAdvice {

    /** Fires when {@code getInputStream()} throws, ensuring the failed request is recorded. */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.This Object connection,
        @Advice.Thrown Throwable thrown
    ) {
      if (thrown == null) {
        return;
      }
      try {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
          classLoader = ClassLoader.getSystemClassLoader();
        }
        Class<?> bridge = classLoader.loadClass("com.rollbar.agent.NetworkEventBridge");

        // Re-entry guard: on connection-level failures (responseCode == -1) the JDK's
        // getResponseCode() calls getInputStream() again, which re-fires this advice. Without the
        // guard that recurses until a StackOverflowError, recording nothing.
        Boolean entered = (Boolean) bridge.getMethod("enterResponseCodeTrigger").invoke(null);
        if (entered == null || !entered) {
          return;
        }
        try {
          connection.getClass().getMethod("getResponseCode").invoke(connection);
        } finally {
          bridge.getMethod("exitResponseCodeTrigger").invoke(null);
        }
      } catch (Throwable ignored) {
        // Advice must never throw
      }
    }
  }

  /**
   * Advice inlined into concrete {@code HttpURLConnection.getErrorStream()}.
   *
   * <p>A non-null return means the server sent a 4xx/5xx response. Invokes
   * {@code getResponseCode()} so that {@link GetResponseCodeAdvice} records the event.
   * Deduplication in {@link com.rollbar.agent.NetworkEventBridge} prevents double-recording.
   */
  public static class GetErrorStreamAdvice {

    /** Fires when {@code getErrorStream()} returns a non-null stream. */
    @Advice.OnMethodExit
    public static void onExit(
        @Advice.This Object connection,
        @Advice.Return Object errorStream
    ) {
      if (errorStream == null) {
        return;
      }
      try {
        connection.getClass().getMethod("getResponseCode").invoke(connection);
      } catch (Throwable ignored) {
        // Advice must never throw
      }
    }
  }

  /**
   * Advice inlined into {@code java.net.HttpURLConnection.getResponseCode()}.
   *
   * <p>Only JDK types are referenced directly. The Rollbar bridge is reached via TCCL
   * to cross the classloader boundary. The connection instance is used as the deduplication
   * key — getResponseCode() is called re-entrantly up to 3 times per request internally.
   */
  public static class GetResponseCodeAdvice {

    /**
     * Fires after {@code getResponseCode()} returns or throws, recording 4xx/5xx as telemetry.
     *
     * <p>Uses the connection instance as a deduplication key since {@code getResponseCode()} is
     * called re-entrantly up to 3 times per request internally.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.This Object connection,
        @Advice.Return int statusCode,
        @Advice.Thrown Throwable thrown
    ) {
      try {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
          cl = ClassLoader.getSystemClassLoader();
        }
        Class<?> bridge = cl.loadClass("com.rollbar.agent.NetworkEventBridge");

        if (thrown != null) {
          Boolean recorded = (Boolean) bridge
              .getMethod("markAsRecorded", Object.class).invoke(null, thrown);
          if (recorded) {
            String msg = thrown.getMessage() != null
                ? thrown.getMessage() : thrown.getClass().getName();
            bridge.getMethod("recordError", String.class).invoke(null, msg);
          }
          return;
        }

        if (statusCode >= 400) {
          Object url = connection.getClass().getMethod("getURL").invoke(connection);
          String urlStr = url != null ? url.toString() : "";
          String method = (String) connection.getClass()
              .getMethod("getRequestMethod").invoke(connection);
          bridge.getMethod("recordNetworkEvent",
                  Object.class, String.class, String.class, String.class)
              .invoke(null, connection, method, urlStr, String.valueOf(statusCode));
        }
      } catch (Throwable ignored) {
        // Advice must never throw — swallow all errors including Error subclasses
      }
    }
  }
}
