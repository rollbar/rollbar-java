package com.rollbar.agent.instrumentation;

import com.rollbar.agent.NetworkEventBridge;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * Installs ByteBuddy advice on Apache HttpClient 4.x to capture network errors.
 */
public final class ApacheHttpClient4Instrumentation {

  private ApacheHttpClient4Instrumentation() {}

  /**
   * Installs a ByteBuddy transformer for Apache HttpClient 4.x.
   *
   * <p>The transformer fires only if a subtype of
   * {@code org.apache.http.impl.client.CloseableHttpClient} is loaded at runtime; if HC4 is absent
   * the registered matcher simply never matches.
   */
  public static void installIfAvailable(AgentBuilder builder, Instrumentation inst) {
    // Always install — ByteBuddy intercepts class loading at the JVM level regardless of which
    // classloader (system, app, or child) eventually loads the client. If HC4 is absent the
    // transformer simply never fires. We must not use Class.forName here: loading
    // CloseableHttpClient before the transformer is installed prevents instrumentation because no
    // RedefinitionStrategy is configured.
    builder
        // doExecute() is declared abstract on CloseableHttpClient and implemented by its subclasses
        // (InternalHttpClient, MinimalHttpClient, third-party wrappers), so we match subtypes.
        // The name filters keep the (relatively costly) hierarchy walk off the JDK classes that
        // reach this matcher because RollbarAgent un-ignores java.* for the JDK HTTP clients.
        .type(ElementMatchers.not(ElementMatchers.nameStartsWith("java."))
            .and(ElementMatchers.not(ElementMatchers.nameStartsWith("javax.")))
            .and(ElementMatchers.not(ElementMatchers.nameStartsWith("jdk.")))
            .and(ElementMatchers.not(ElementMatchers.nameStartsWith("sun.")))
            .and(ElementMatchers.hasSuperType(
                ElementMatchers.named("org.apache.http.impl.client.CloseableHttpClient"))))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(DoExecuteAdvice.class)
                // Target doExecute(HttpHost, HttpRequest, HttpContext) — the real convergence point
                // of every dispatch path. Verified against httpclient-4.5.14 bytecode: the
                // HttpUriRequest overloads reach it via determineTarget(), the HttpHost overloads
                // invoke it directly, and the ResponseHandler overloads route through
                // execute(HttpHost, HttpRequest, HttpContext). Instrumenting any public execute()
                // overload instead would miss the paths that bypass it. Exactly one advice
                // invocation per request, whichever overload the caller used.
                .on(ElementMatchers.named("doExecute")
                    .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                    .and(ElementMatchers.not(ElementMatchers.isBridge()))
                    .and(ElementMatchers.takesArgument(0,
                        ElementMatchers.named("org.apache.http.HttpHost")))
                    .and(ElementMatchers.takesArgument(1,
                        ElementMatchers.named("org.apache.http.HttpRequest")))
                    .and(ElementMatchers.takesArgument(2,
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
   * concatenation, which ByteBuddy cannot inline into a Java 6 class file. Delegating to
   * {@link NetworkEventBridge} keeps concatenation out of the inlined code entirely.
   */
  public static class DoExecuteAdvice {

    /**
     * Fires after {@code doExecute(HttpHost, HttpRequest, HttpContext)} returns or throws,
     * recording 4xx/5xx responses as telemetry.
     *
     * <p>The response (or the thrown exception) is the deduplication key, so a client that wraps
     * another {@code CloseableHttpClient} — where the outer and inner {@code doExecute} both fire
     * for one request — still records a single event.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) HttpHost target,
        @Advice.Argument(1) HttpRequest request,
        @Advice.Return HttpResponse response,
        @Advice.Thrown Throwable thrown
    ) {
      try {
        if (thrown != null) {
          if (NetworkEventBridge.markAsRecorded(thrown)) {
            String message = thrown.getMessage() != null
                ? thrown.getMessage() : thrown.getClass().getName();
            NetworkEventBridge.recordError(message);
          }
          return;
        }

        if (response != null && request != null) {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode >= 400) {
            // The host-based overloads carry the target separately from a request whose URI may be
            // just a path, so rejoin the two rather than reading the request URI alone.
            String base = target != null ? target.toURI() : null;
            String requestUri = request.getRequestLine() != null
                ? request.getRequestLine().getUri() : null;
            NetworkEventBridge.recordNetworkEvent(
                response,
                request.getRequestLine().getMethod(),
                NetworkEventBridge.composeUrl(base, requestUri),
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
