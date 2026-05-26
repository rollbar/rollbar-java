package com.rollbar.agent.instrumentation;

import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.UrlSanitizer;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * Installs ByteBuddy advice on Apache HttpClient 4.x to capture network errors.
 */
public final class ApacheHttpClient4Instrumentation {

  private ApacheHttpClient4Instrumentation() {}

  /**
   * Instruments Apache HttpClient 4.x if present on the classpath.
   *
   * <p>Does nothing if {@code org.apache.http.impl.client.CloseableHttpClient} is not available.
   */
  public static void installIfAvailable(AgentBuilder builder, Instrumentation inst) {
    try {
      Class.forName("org.apache.http.impl.client.CloseableHttpClient");
    } catch (ClassNotFoundException e) {
      return;
    }

    builder
        .type(ElementMatchers.hasSuperType(
            ElementMatchers.named("org.apache.http.impl.client.CloseableHttpClient")))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(ExecuteAdvice.class)
                .on(ElementMatchers.named("execute")
                    .and(ElementMatchers.takesArgument(0,
                        ElementMatchers.named("org.apache.http.client.methods.HttpUriRequest")))))
        )
        .installOn(inst);
  }

  /**
   * Apache HC 4.x runs in the application classloader, so we can reference Rollbar classes
   * directly without the TCCL reflection bridge.
   */
  public static class ExecuteAdvice {

    /**
     * Fires after {@code execute()} returns or throws, recording 4xx/5xx responses as telemetry.
     *
     * <p>Apache HC 4.x runs in the application classloader, so Rollbar classes are referenced
     * directly without the TCCL reflection bridge needed for JDK instrumentation.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) HttpUriRequest request,
        @Advice.Return HttpResponse response,
        @Advice.Thrown Throwable thrown
    ) {
      try {
        if (thrown != null) {
          String message = thrown.getMessage() != null
              ? thrown.getMessage() : thrown.getClass().getName();
          AgentTelemetryStore.getInstance().recordManualEventFor(
              Level.CRITICAL,
              Source.SERVER,
              "Network error: " + message
          );
          return;
        }

        if (response != null) {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode >= 400) {
            String url = request.getURI() != null ? request.getURI().toString() : "";
            AgentTelemetryStore.getInstance().recordNetworkEventFor(
                Level.CRITICAL,
                Source.SERVER,
                request.getMethod(),
                UrlSanitizer.sanitize(url),
                String.valueOf(statusCode)
            );
          }
        }
      } catch (Throwable ignored) {
        // Advice must never throw
      }
    }
  }
}
