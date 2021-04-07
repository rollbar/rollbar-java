package com.rollbar.reactivestreams.notifier.sender.http;

import org.reactivestreams.Publisher;

/**
 * General interface for non-blocking reactivestreams HTTP client implementations.
 */
public interface AsyncHttpClient extends AutoCloseable {
  /**
   * Execute a HTTP request.
   *
   * <p>
   *   The returned publisher *MUST* signal {@link org.reactivestreams.Subscriber#onNext} for
   *   any valid responses, including ones with error status codes (&gt;= 400). It should only
   *   signal {@link org.reactivestreams.Subscriber#onError(Throwable)} when the request / response
   *   exchange could not be completed.
   * </p>
   *
   * @param httpRequest The request.
   * @return A mono publisher that will execute the HTTP request once the first element is
   *         requested.
   */
  Publisher<AsyncHttpResponse> send(AsyncHttpRequest httpRequest);

  /**
   * Closes this client.
   *
   * <p>
   *   Note: the {@link AutoCloseable#close()} implementation should call this method with a false
   *   wait argument, or include equivalent logic.
   * </p>
   *
   * @param wait If true, the close method will block until pending HTTP operations complete.
   */
  void close(boolean wait);

}
