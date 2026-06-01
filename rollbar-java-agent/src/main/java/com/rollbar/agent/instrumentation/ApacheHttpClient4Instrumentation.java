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
    // Use a resource check rather than Class.forName: execute(HttpUriRequest, HttpContext) is a
    // concrete method defined in CloseableHttpClient itself, so loading that class before the
    // ByteBuddy transformer is installed prevents the method from ever being instrumented (no
    // RedefinitionStrategy is configured). getResource checks classpath availability without
    // triggering class loading.
    if (ClassLoader.getSystemClassLoader().getResource(
        "org/apache/http/impl/client/CloseableHttpClient.class") == null) {
      return;
    }

    builder
        .type(ElementMatchers.named("org.apache.http.impl.client.CloseableHttpClient"))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(ExecuteAdvice.class)
                .on(ElementMatchers.named("execute")
                    // Target only execute(HttpUriRequest, HttpContext) — the single concrete
                    // method that all other execute() overloads delegate to before calling
                    // doExecute(). This ensures exactly one advice invocation per HTTP request
                    // regardless of which public overload the caller uses (including
                    // ResponseHandler
                    // variants). The bridge method for this overload is excluded to avoid a second
                    // firing; the 1 arg execute(HttpUriRequest) is excluded because it has no arg
                    // at index 1, and ResponseHandler overloads are excluded because their second
                    // arg is ResponseHandler, not HttpContext.
                    .and(ElementMatchers.not(ElementMatchers.isBridge()))
                    .and(ElementMatchers.takesArgument(0,
                        ElementMatchers.named("org.apache.http.client.methods.HttpUriRequest")))
                    .and(ElementMatchers.takesArgument(1,
                        ElementMatchers.named("org.apache.http.protocol.HttpContext")))))
        )
        .installOn(inst);
  }

  /**
   * Apache HC 4.x runs in the application classloader, so we can reference Rollbar classes
   * directly without the TCCL reflection bridge.
   *
   * <p>String concatenation in the advice body must use {@link String#concat} or
   * {@link StringBuilder} rather than the {@code +} operator. Apache HC 4.x jars are compiled at
   * class-file version 50 (Java 6); the Java 9+ compiler emits {@code invokedynamic} for {@code +}
   * concatenation, which ByteBuddy cannot inline into a Java 6 class file.
   */
  public static class ExecuteAdvice {

    /**
     * Fires after {@code execute(HttpUriRequest, HttpContext)} returns or throws, recording
     * 4xx/5xx responses as telemetry.
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
          // Use String.concat instead of + to avoid invokedynamic
          // (unsupported in Java 6 class files)
          AgentTelemetryStore.getInstance().recordManualEventFor(
              Level.CRITICAL,
              Source.SERVER,
              "Network error: ".concat(message)
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
