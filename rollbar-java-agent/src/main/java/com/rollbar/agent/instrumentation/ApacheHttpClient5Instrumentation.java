package com.rollbar.agent.instrumentation;

import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.UrlSanitizer;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import java.lang.instrument.Instrumentation;
import java.net.URI;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;

/**
 * Installs ByteBuddy advice on Apache HttpClient 5.x to capture network errors.
 */
public final class ApacheHttpClient5Instrumentation {

  private ApacheHttpClient5Instrumentation() {}

  /**
   * Installs a ByteBuddy transformer for Apache HttpClient 5.x.
   *
   * <p>The transformer fires only if
   * {@code org.apache.hc.client5.http.impl.classic.CloseableHttpClient} is loaded at runtime;
   * if HC5 is absent the registered matcher simply never matches.
   */
  public static void installIfAvailable(AgentBuilder builder, Instrumentation inst) {
    // Always install — ByteBuddy intercepts class loading at the JVM level regardless of which
    // classloader (system, app, or child) eventually loads CloseableHttpClient. If HC5 is absent
    // the transformer simply never fires. We must not use Class.forName here: loading
    // CloseableHttpClient before the transformer is installed prevents instrumentation because no
    // RedefinitionStrategy is configured.
    builder
        .type(ElementMatchers.named("org.apache.hc.client5.http.impl.classic.CloseableHttpClient"))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(ExecuteAdvice.class)
                .on(ElementMatchers.named("execute")
                    // Exclude bridge methods generated for covariant return-type overrides — they
                    // return HttpResponse (supertype), which @Advice.Return ClassicHttpResponse
                    // cannot safely bind via downcast.
                    .and(ElementMatchers.not(ElementMatchers.isBridge()))
                    .and(ElementMatchers.takesArgument(0,
                        ElementMatchers.named("org.apache.hc.core5.http.ClassicHttpRequest")))
                    // Exclude HttpClientResponseHandler overloads: they return generic T erased to
                    // Object, which cannot be bound to @Advice.Return ClassicHttpResponse and would
                    // cause ByteBuddy to fail the transformation for the whole CloseableHttpClient.
                    // HC5 has both 2-arg (arg1=handler) and 3-arg (arg1=context, arg2=handler)
                    // variants; both are excluded here.
                    .and(ElementMatchers.not(ElementMatchers.takesArgument(1,
                        ElementMatchers.named(
                            "org.apache.hc.core5.http.io.HttpClientResponseHandler"))))
                    .and(ElementMatchers.not(ElementMatchers.takesArgument(2,
                        ElementMatchers.named(
                            "org.apache.hc.core5.http.io.HttpClientResponseHandler"))))))
        )
        .installOn(inst);
  }

  /**
   * Apache HC 5.x runs in the application classloader, so we can reference Rollbar classes
   * directly without the TCCL reflection bridge.
   */
  public static class ExecuteAdvice {

    /**
     * Fires after {@code execute()} returns or throws, recording 4xx/5xx responses as telemetry.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) ClassicHttpRequest request,
        @Advice.Return ClassicHttpResponse response,
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
          int statusCode = response.getCode();
          if (statusCode >= 400) {
            String url;
            try {
              URI uri = request.getUri();
              url = uri != null ? uri.toString() : "";
            } catch (java.net.URISyntaxException ignored) {
              url = request.getRequestUri() != null ? request.getRequestUri() : "";
            }
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
