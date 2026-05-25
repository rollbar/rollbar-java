package com.rollbar.agent.instrumentation;

import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.UrlSanitizer;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.lang.instrument.Instrumentation;

public final class ApacheHttpClient5Instrumentation {

  private ApacheHttpClient5Instrumentation() {}

  public static void installIfAvailable(AgentBuilder builder, Instrumentation inst) {
    try {
      Class.forName("org.apache.hc.client5.http.impl.classic.CloseableHttpClient");
    } catch (ClassNotFoundException e) {
      return;
    }

    builder
        .type(ElementMatchers.hasSuperType(
            ElementMatchers.named("org.apache.hc.client5.http.impl.classic.CloseableHttpClient")))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(ExecuteAdvice.class)
                .on(ElementMatchers.named("execute")
                    .and(ElementMatchers.takesArgument(0,
                        ElementMatchers.named("org.apache.hc.core5.http.ClassicHttpRequest")))))
        )
        .installOn(inst);
  }

  /**
   * Apache HC 5.x runs in the application classloader, so we can reference Rollbar classes
   * directly without the TCCL reflection bridge.
   */
  public static class ExecuteAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) ClassicHttpRequest request,
        @Advice.Return ClassicHttpResponse response,
        @Advice.Thrown Throwable thrown
    ) {
      try {
        if (thrown != null) {
          AgentTelemetryStore.getInstance().recordManualEventFor(
              Level.CRITICAL,
              Source.SERVER,
              "Network error: " + (thrown.getMessage() != null ? thrown.getMessage() : thrown.getClass().getName())
          );
          return;
        }

        if (response != null) {
          int statusCode = response.getCode();
          if (statusCode >= 400) {
            String url = request.getRequestUri() != null ? request.getRequestUri() : "";
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
