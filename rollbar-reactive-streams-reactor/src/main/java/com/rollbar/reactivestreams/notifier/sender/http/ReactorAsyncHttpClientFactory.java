package com.rollbar.reactivestreams.notifier.sender.http;

class ReactorAsyncHttpClientFactory implements HttpClientFactory {
  @Override
  public AsyncHttpClient build() {
    return new ReactorAsyncHttpClient.Builder().build();
  }
}
