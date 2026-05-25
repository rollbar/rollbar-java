package com.rollbar.agent.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public final class HttpURLConnectionInstrumentation {

  private HttpURLConnectionInstrumentation() {}

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
            String msg = thrown.getMessage() != null ? thrown.getMessage() : thrown.getClass().getName();
            bridge.getMethod("recordError", String.class).invoke(null, msg);
          }
          return;
        }

        if (statusCode >= 400) {
          Object url = connection.getClass().getMethod("getURL").invoke(connection);
          String urlStr = url != null ? url.toString() : "";
          String method = (String) connection.getClass().getMethod("getRequestMethod").invoke(connection);
          // connection instance as dedup key — same object across all re-entrant getResponseCode() calls
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
