package com.rollbar.reactivestreams.notifier.sender.http;

/**
 * Factory class for {@link ApacheAsyncHttpClient}.
 */
class ApacheAsyncHttpClientFactory implements HttpClientFactory {
  @Override
  public AsyncHttpClient build() {
    return new ApacheAsyncHttpClient.Builder().build();
  }
}
