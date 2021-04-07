package com.rollbar.reactivestreams.notifier.sender.http;

import java.util.Map;

/**
 * A HTTP server response to an asynchronous request.
 */
public interface AsyncHttpResponse {
  int getStatusCode();

  Iterable<Map.Entry<String, String>> getHeaders();

  String getBody();
}
