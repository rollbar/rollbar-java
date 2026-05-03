package com.rollbar.reactivestreams.notifier.sender.http;

import java.util.Map;

/**
 * Data for an asynchronous, non-blocking HTTP request.
 */
class AsyncHttpRequestImpl implements AsyncHttpRequest {
  private final String url;
  private final Iterable<Map.Entry<String, String>> headers;
  private final String body;
  private final byte[] bodyBytes;

  public AsyncHttpRequestImpl(String url, Iterable<Map.Entry<String, String>> headers,
                              String body) {
    this.url = url;
    this.headers = headers;
    this.body = body;
    this.bodyBytes = null;
  }

  public AsyncHttpRequestImpl(String url, Iterable<Map.Entry<String, String>> headers,
                              byte[] bodyBytes) {
    this.url = url;
    this.headers = headers;
    this.body = null;
    this.bodyBytes = bodyBytes;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public Iterable<Map.Entry<String, String>> getHeaders() {
    return headers;
  }

  @Override
  public String getBody() {
    return body;
  }

  @Override
  public byte[] getBodyBytes() {
    return bodyBytes;
  }
}
