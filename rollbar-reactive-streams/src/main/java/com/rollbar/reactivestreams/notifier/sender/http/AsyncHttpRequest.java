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

  default byte[] getBodyBytes() {
    return null;
  }

  class Builder {
    public static AsyncHttpRequest build(String url, Set<Map.Entry<String, String>> headers,
                                         String reqBody) {
      return new AsyncHttpRequestImpl(url, headers, reqBody);
    }

    public static AsyncHttpRequest build(String url, Set<Map.Entry<String, String>> headers,
                                         byte[] body) {
      return new AsyncHttpRequestImpl(url, headers, body);
    }
  }
}
