package com.rollbar.api.payload.data;

import static java.util.Arrays.asList;

import static com.rollbar.test.Factory.request;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
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
    Map<String, List<String>> get = new HashMap<>();
    get.put("param1", asList("value1.1", "value1.2"));
    get.put("param2", asList("value2.1"));
    Map<String, Object> expectedGet = new HashMap<>();
    expectedGet.put("param1", get.get("param1"));
    expectedGet.put("param2", "value2.1");

    Request request = request(get);

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
      expected.put("get", expectedGet);
    }
    if (request.getQueryString() != null) {
      expected.put("query_string", request.getQueryString());
    }
    if (request.getPost() != null) {
      expected.put("post", request.getPost());
    }
    if (request.getBody() != null) {
      expected.put("body", request.getBody());
    }
    if (request.getUserIp() != null) {
      expected.put("user_ip", request.getUserIp());
    }

    assertThat(request.asJson(), is(expected));
  }

  @Test
  public void shouldAllowMetadata() {
    Map<String, List<String>> get = new HashMap<>();
    get.put("param1", asList("value1.1", "value1.2"));
    get.put("param2", asList("value2.1"));
    Map<String, Object> expectedGet = new HashMap<>();
    expectedGet.put("param1", get.get("param1"));
    expectedGet.put("param2", "value2.1");

    Request baseRequest = request(get);

    Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("a", "b");
    metadataMap.put("num", 42);
    metadataMap.put("url", "should not show up");

    Request request = new Request.Builder(baseRequest)
      .metadata(metadataMap)
      .build();

    Map<String, Object> expected = new HashMap<>();
    expected.put("a", "b");
    expected.put("num", 42);

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
      expected.put("get", expectedGet);
    }
    if (request.getQueryString() != null) {
      expected.put("query_string", request.getQueryString());
    }
    if (request.getPost() != null) {
      expected.put("post", request.getPost());
    }
    if (request.getBody() != null) {
      expected.put("body", request.getBody());
    }
    if (request.getUserIp() != null) {
      expected.put("user_ip", request.getUserIp());
    }

    assertThat(request.asJson(), is(expected));
  }
}
