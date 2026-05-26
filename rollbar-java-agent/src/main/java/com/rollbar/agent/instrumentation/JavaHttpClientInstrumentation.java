package com.rollbar.agent.instrumentation;

import java.lang.instrument.Instrumentation;
import java.net.http.HttpClient;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Installs ByteBuddy advice on {@code java.net.http.HttpClient} subtypes to capture network errors.
 */
public final class JavaHttpClientInstrumentation {

  private JavaHttpClientInstrumentation() {}

  /**
   * Instruments {@code java.net.http.HttpClient} subtypes if available on the current JVM.
   *
   * <p>Does nothing if {@code java.net.http.HttpClient} is not present (i.e. below Java 11).
   */
  public static void installIfAvailable(AgentBuilder builder, Instrumentation inst) {
    try {
      Class.forName("java.net.http.HttpClient");
    } catch (ClassNotFoundException e) {
      return;
    }

    builder
        .type(ElementMatchers.isSubTypeOf(HttpClient.class))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(SendAdvice.class)
                .on(ElementMatchers.named("send")))
        )
        .installOn(inst);
  }

  /**
   * Advice inlined into JDK's HttpClient concrete implementation's send().
   *
   * <p>Only JDK types are referenced directly. The Rollbar bridge is reached via TCCL
   * to cross the classloader boundary. The response object is used as a deduplication key
   * since both HttpClientFacade and HttpClientImpl instrument send() and the same response
   * object flows through both.
   */
  public static class SendAdvice {

    /**
     * Fires after {@code send()} returns or throws, recording 4xx/5xx responses as telemetry.
     *
     * <p>Uses the response object as a deduplication key to avoid duplicate events when both
     * {@code HttpClientFacade} and {@code HttpClientImpl} invoke this advice.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) Object request,
        @Advice.Return Object response,
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
            String message = thrown.getMessage() != null
                ? thrown.getMessage() : thrown.getClass().getName();
            bridge.getMethod("recordError", String.class).invoke(null, message);
          }
          return;
        }

        if (response != null) {
          int statusCode = (Integer) response.getClass().getMethod("statusCode").invoke(response);
          if (statusCode >= 400) {
            Object uri = request.getClass().getMethod("uri").invoke(request);
            String method = (String) request.getClass().getMethod("method").invoke(request);
            // response object is the dedup key — unique per send() call, shared between
            // HttpClientFacade and HttpClientImpl so only one event is recorded
            bridge.getMethod("recordNetworkEvent",
                    Object.class, String.class, String.class, String.class)
                .invoke(null, response, method, uri.toString(), String.valueOf(statusCode));
          }
        }
      } catch (Throwable ignored) {
        // Advice must never throw — swallow all errors including Error subclasses
      }
    }
  }
}
