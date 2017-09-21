package com.rollbar.api.payload.data;

import static com.rollbar.test.Factory.request;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class RequestTest {

  @Test
  public void shouldBeEqual() {
    Request request1 = request();
    Request request2 = request();

    assertThat(request2, is(request1));
  }

  @Test
  public void shouldReturnAsJson() {
    Request request = request();

    Map<String, Object> expected = new HashMap<>();

    if (request.getUrl() != null) {
      expected.put("url", request.getUrl());
    }
    if (request.getHeaders() != null) {
      expected.put("headers", request.getHeaders());
    }
    if (request.getParams() != null) {
      expected.put("params", request.getParams());
    }
    if (request.getGet() != null) {
      expected.put("get", request.getGet());
    }
    if (request.getQueryString() != null) {
      expected.put("queryString", request.getQueryString());
    }
    if (request.getPost() != null) {
      expected.put("post", request.getPost());
    }
    if (request.getBody() != null) {
      expected.put("body", request.getBody());
    }
    if (request.getUserIp() != null) {
      expected.put("userIp", request.getUserIp());
    }

    assertThat(request.asJson(), is(expected));
  }
}