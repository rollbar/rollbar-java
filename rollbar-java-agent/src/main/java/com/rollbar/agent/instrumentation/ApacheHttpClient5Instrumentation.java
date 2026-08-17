package com.rollbar.agent.instrumentation;

import com.rollbar.agent.NetworkEventBridge;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

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
   * Records a 4xx/5xx HC 5.x response as telemetry; other statuses are ignored. Called by
   * {@link DoExecuteAdvice}, which cannot name {@code org.apache.hc} types itself.
   *
   * <p>Members are read through the public {@code org.apache.hc.core5.http} interfaces rather than
   * the concrete class of each object, because HC 5.x hands back package-private implementations —
   * {@code doExecute} returns the adapter {@code CloseableHttpResponse.adapt} produces — and a
   * {@link java.lang.reflect.Method} looked up on such a class cannot be invoked. The reflection
   * runs once per request, which is immaterial next to the HTTP call it describes.
   *
   * @param target the request target the client dispatched to, or null
   * @param request the executed request
   * @param response the response returned by {@code doExecute}
   */
  public static void recordResponse(Object target, Object request, Object response) {
    try {
      int statusCode =
          (Integer) invokeVia("org.apache.hc.core5.http.HttpResponse", response, "getCode");
      if (statusCode < 400) {
        return;
      }
      String requestUri;
      try {
        Object uri = invokeVia("org.apache.hc.core5.http.HttpRequest", request, "getUri");
        requestUri = uri != null ? uri.toString() : null;
      } catch (InvocationTargetException ignored) {
        // getUri() throws URISyntaxException for a request URI HC could not assemble; the raw
        // request URI is still worth recording.
        requestUri =
            (String) invokeVia("org.apache.hc.core5.http.HttpRequest", request, "getRequestUri");
      }
      String method =
          (String) invokeVia("org.apache.hc.core5.http.HttpRequest", request, "getMethod");
      // The host-based overloads carry the target separately from a request whose URI may be
      // just a path, so rejoin the two rather than reading the request URI alone.
      String base = target != null
          ? (String) invokeVia("org.apache.hc.core5.http.HttpHost", target, "toURI") : null;
      NetworkEventBridge.recordNetworkEvent(
          response,
          method,
          NetworkEventBridge.composeUrl(base, requestUri),
          String.valueOf(statusCode)
      );
    } catch (Throwable ignored) {
      // Telemetry must never disrupt the instrumented request
    }
  }

  /**
   * Invokes {@code methodName} on {@code receiver} through the named public API type, resolved from
   * the receiver's own classloader — the one that loaded HC 5.x, which the agent's classloader may
   * not be able to see.
   */
  private static Object invokeVia(String apiTypeName, Object receiver, String methodName)
      throws ReflectiveOperationException {
    ClassLoader classLoader = receiver.getClass().getClassLoader();
    Class<?> apiType = Class.forName(apiTypeName, false,
        classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
    return apiType.getMethod(methodName).invoke(receiver);
  }

  /**
   * Apache HC 5.x runs in the application classloader, so the advice body can reference Rollbar
   * classes directly without the TCCL reflection bridge.
   *
   * <p>The advice signature, however, must not name {@code org.apache.hc} types.
   * {@code Advice.to(DoExecuteAdvice.class)} resolves this method's parameter and return types via
   * {@link Class#getDeclaredMethods()}, in the classloader that loaded the advice class — the
   * agent's, which under {@code -javaagent} is the system classloader. Wherever HC 5.x is loaded by
   * a child classloader the agent cannot see (Spring Boot executable jars, per-WAR container
   * classloaders, OSGi bundles), that lookup throws {@link NoClassDefFoundError} inside the
   * transformer; the AgentBuilder reports it and moves on, and HC 5.x is silently never
   * instrumented. So everything the advice touches is typed {@link Object} and read reflectively in
   * {@link ApacheHttpClient5Instrumentation#recordResponse}, which runs after the weave and can
   * resolve those types from the client's own classloader.
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
        @Advice.Argument(0) Object target,
        @Advice.Argument(1) Object request,
        @Advice.Return Object response,
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
          ApacheHttpClient5Instrumentation.recordResponse(target, request, response);
        }
      } catch (Throwable ignored) {
        // Advice must never throw
      }
    }
  }
}
