package com.rollbar.agent.instrumentation;

import com.rollbar.agent.NetworkEventBridge;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

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
   * Records a 4xx/5xx HC 4.x response as telemetry; other statuses are ignored. Called by
   * {@link DoExecuteAdvice}, which cannot name {@code org.apache.http} types itself.
   *
   * <p>Members are read through the public {@code org.apache.http} interfaces rather than the
   * concrete class of each object, because HC 4.x hands back package-private implementations —
   * {@code doExecute} returns {@code org.apache.http.impl.execchain.HttpResponseProxy} — and a
   * {@link java.lang.reflect.Method} looked up on such a class cannot be invoked. The reflection
   * runs once per request, which is immaterial next to the HTTP call it describes.
   *
   * @param target the request target the client dispatched to, or null
   * @param request the executed request
   * @param response the response returned by {@code doExecute}
   */
  public static void recordResponse(Object target, Object request, Object response) {
    try {
      Object statusLine = invokeVia("org.apache.http.HttpResponse", response, "getStatusLine");
      if (statusLine == null) {
        return;
      }
      int statusCode =
          (Integer) invokeVia("org.apache.http.StatusLine", statusLine, "getStatusCode");
      if (statusCode < 400) {
        return;
      }
      Object requestLine = invokeVia("org.apache.http.HttpRequest", request, "getRequestLine");
      if (requestLine == null) {
        return;
      }
      String method = (String) invokeVia("org.apache.http.RequestLine", requestLine, "getMethod");
      String requestUri = (String) invokeVia("org.apache.http.RequestLine", requestLine, "getUri");
      // The host-based overloads carry the target separately from a request whose URI may be
      // just a path, so rejoin the two rather than reading the request URI alone.
      String base = target != null
          ? (String) invokeVia("org.apache.http.HttpHost", target, "toURI") : null;
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
   * the receiver's own classloader — the one that loaded HC 4.x, which the agent's classloader may
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
   * Apache HC 4.x runs in the application classloader, so the advice body can reference Rollbar
   * classes directly without the TCCL reflection bridge.
   *
   * <p>The advice signature, however, must not name {@code org.apache.http} types.
   * {@code Advice.to(DoExecuteAdvice.class)} resolves this method's parameter and return types via
   * {@link Class#getDeclaredMethods()}, in the classloader that loaded the advice class — the
   * agent's, which under {@code -javaagent} is the system classloader. Wherever HC 4.x is loaded by
   * a child classloader the agent cannot see (Spring Boot executable jars, per-WAR container
   * classloaders, OSGi bundles), that lookup throws {@link NoClassDefFoundError} inside the
   * transformer; the AgentBuilder reports it and moves on, and HC 4.x is silently never
   * instrumented. So everything the advice touches is typed {@link Object} and read reflectively in
   * {@link ApacheHttpClient4Instrumentation#recordResponse}, which runs after the weave and can
   * resolve those types from the client's own classloader.
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
          ApacheHttpClient4Instrumentation.recordResponse(target, request, response);
        }
      } catch (Throwable ignored) {
        // Advice must never throw
      }
    }
  }
}
