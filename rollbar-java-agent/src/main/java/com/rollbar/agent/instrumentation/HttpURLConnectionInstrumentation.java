package com.rollbar.agent.instrumentation;

import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Installs ByteBuddy advice on {@code java.net.HttpURLConnection} to capture network errors.
 */
public final class HttpURLConnectionInstrumentation {

  private HttpURLConnectionInstrumentation() {}

  /**
   * Instruments {@code java.net.HttpURLConnection.getResponseCode()} to record 4xx/5xx responses.
   *
   * <p>Targets the declaring class directly to capture the method regardless of concrete subtype.
   */
  public static void install(AgentBuilder builder, Instrumentation inst) {
    builder
        .type(ElementMatchers.named("java.net.HttpURLConnection"))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(GetResponseCodeAdvice.class)
                .on(ElementMatchers.named("getResponseCode")))
        )
        .installOn(inst);
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
