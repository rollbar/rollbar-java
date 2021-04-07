package com.rollbar.reactivestreams.notifier.sender.http;

import java.util.Map;
import java.util.Set;

/**
 * Data for an asynchronous, non-blocking HTTP request.
 */
public interface AsyncHttpRequest {
  String getUrl();

  Iterable<Map.Entry<String, String>> getHeaders();

  String getBody();

  class Builder {
    public static AsyncHttpRequest build(String url, Set<Map.Entry<String, String>> headers,
                                         String reqBody) {
      return new AsyncHttpRequestImpl(url, headers, reqBody);
    }
  }
}
