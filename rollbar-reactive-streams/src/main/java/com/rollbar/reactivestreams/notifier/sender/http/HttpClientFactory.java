package com.rollbar.reactivestreams.notifier.sender.http;

interface HttpClientFactory {
  AsyncHttpClient build();
}
