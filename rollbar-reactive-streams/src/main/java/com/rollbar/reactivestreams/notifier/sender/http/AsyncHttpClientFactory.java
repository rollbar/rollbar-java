package com.rollbar.reactivestreams.notifier.sender.http;

/**
 * Builds {@link HttpClientFactory} instances based on classes available in the classpath.
 *
 * <p>
 *   It currently supports Apache HTTP Components directly, which is compatible with Java 7.
 *   A Project Reactor implementation will become available if rollbar-reactive-streams-reactor
 *   is available in the classpath. The Reactor implementation requires Java 8.
 * </p>
 * <p>
 *   Each implementation requires that the corresponding client library be available in the
 *   classpath:
 * </p>
 *   <ul>
 *     <li>org.apache.httpcomponents.client5:httpclient5 for the Apache HTTP implementation</li>
 *     <li>io.projectreactor.netty:reactor-netty-http for the Reactor implementation</li>
 *   </ul>
 */
public class AsyncHttpClientFactory {
  private static final HttpClientFactory reactorFactory;
  private static final HttpClientFactory apacheHttpFactory;

  static {
    reactorFactory = createFactoryOrNull(
        "com.rollbar.reactivestreams.notifier.sender.http.ReactorAsyncHttpClientFactory");

    if (isPresent("org.apache.hc.client5.http.async.HttpAsyncClient")) {
      apacheHttpFactory = new ApacheAsyncHttpClientFactory();
    } else {
      apacheHttpFactory = null;
    }
  }

  private static boolean isPresent(String className) {
    try {
      Class.forName(className, false, AsyncHttpClientFactory.class.getClassLoader());
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static HttpClientFactory createFactoryOrNull(String factoryClassName) {
    try {
      Class<?> clazz =
          Class.forName(factoryClassName, false, AsyncHttpClientFactory.class.getClassLoader());
      return (HttpClientFactory) clazz.getDeclaredConstructor().newInstance();
    } catch (Throwable ignored) {
      return null;
    }
  }

  /**
   * Builds an AsyncHttpClient based on available classes in the classpath.
   *
   * <p>
   *   The Reactor implementation, if available, will be preferred.
   * </p>
   *
   * @return a AsyncHttpClient instance.
   * @throws IllegalStateException if no AsyncHttpClient can be built.
   */
  public static AsyncHttpClient defaultClient() {
    if (reactorFactory != null) {
      return reactorFactory.build();
    } else if (apacheHttpFactory != null) {
      return apacheHttpFactory.build();
    }

    throw new IllegalStateException("No compatible async HTTP clients found in the classpath.");
  }
}
