package com.rollbar.agent.instrumentation;

import com.rollbar.agent.NetworkEventBridge;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;

/**
 * Installs ByteBuddy advice on Apache HttpClient 5.x to capture network errors.
 */
public final class ApacheHttpClient5Instrumentation {

  private ApacheHttpClient5Instrumentation() {}

  /**
   * Installs a ByteBuddy transformer for Apache HttpClient 5.x.
   *
   * <p>The transformer fires only if a subtype of
   * {@code org.apache.hc.client5.http.impl.classic.CloseableHttpClient} is loaded at runtime; if
   * HC5 is absent the registered matcher simply never matches.
   */
  public static void installIfAvailable(AgentBuilder builder, Instrumentation inst) {
    // Always install — ByteBuddy intercepts class loading at the JVM level regardless of which
    // classloader (system, app, or child) eventually loads the client. If HC5 is absent the
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
            .and(ElementMatchers.hasSuperType(ElementMatchers.named(
                "org.apache.hc.client5.http.impl.classic.CloseableHttpClient"))))
        .transform((b, typeDescription, classLoader, module, protectionDomain) ->
            b.visit(Advice.to(DoExecuteAdvice.class)
                // Target doExecute(HttpHost, ClassicHttpRequest, HttpContext) — the real
                // convergence point of every dispatch path. Verified against httpclient5-5.3.1
                // bytecode: the request-only overloads reach it via determineTarget(), the HttpHost
                // overloads invoke it directly, and the HttpClientResponseHandler overloads route
                // through execute(HttpHost, ClassicHttpRequest, HttpContext, handler), which calls
                // it too. Instrumenting any public execute() overload instead would miss the paths
                // that bypass it, and the handler overloads erase their return type to Object,
                // which cannot bind to @Advice.Return. doExecute() has a single concrete signature,
                // so both problems disappear.
                .on(ElementMatchers.named("doExecute")
                    .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                    .and(ElementMatchers.not(ElementMatchers.isBridge()))
                    .and(ElementMatchers.takesArgument(0,
                        ElementMatchers.named("org.apache.hc.core5.http.HttpHost")))
                    .and(ElementMatchers.takesArgument(1,
                        ElementMatchers.named("org.apache.hc.core5.http.ClassicHttpRequest")))
                    .and(ElementMatchers.takesArgument(2,
                        ElementMatchers.named("org.apache.hc.core5.http.protocol.HttpContext")))))
        )
        .installOn(inst);
  }

  /**
   * Apache HC 5.x runs in the application classloader, so we can reference Rollbar classes
   * directly without the TCCL reflection bridge.
   */
  public static class DoExecuteAdvice {

    /**
     * Fires after {@code doExecute(HttpHost, ClassicHttpRequest, HttpContext)} returns or throws,
     * recording 4xx/5xx responses as telemetry.
     *
     * <p>The response (or the thrown exception) is the deduplication key, so a client that wraps
     * another {@code CloseableHttpClient} — where the outer and inner {@code doExecute} both fire
     * for one request — still records a single event.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) HttpHost target,
        @Advice.Argument(1) ClassicHttpRequest request,
        @Advice.Return ClassicHttpResponse response,
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
          int statusCode = response.getCode();
          if (statusCode >= 400) {
            String requestUri;
            try {
              URI uri = request.getUri();
              requestUri = uri != null ? uri.toString() : null;
            } catch (URISyntaxException ignored) {
              requestUri = request.getRequestUri();
            }
            // The host-based overloads carry the target separately from a request whose URI may be
            // just a path, so rejoin the two rather than reading the request URI alone.
            String base = target != null ? target.toURI() : null;
            NetworkEventBridge.recordNetworkEvent(
                response,
                request.getMethod(),
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
