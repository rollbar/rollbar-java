package com.rollbar.agent.instrumentation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.NetworkEventBridge;
import com.rollbar.api.payload.data.TelemetryEvent;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class ApacheHttpClient5InstrumentationTest {

  private WireMockServer server;
  private CloseableHttpClient client;

  @BeforeEach
  public void setUp() {
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    client = HttpClients.createDefault();
    AgentTelemetryStore.initForTesting(System::currentTimeMillis);
    NetworkEventBridge.resetRecordedForTesting();
  }

  @AfterEach
  public void tearDown() throws Exception {
    client.close();
    server.stop();
  }

  @Test
  public void successResponse_doesNotRecordEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/ok")).willReturn(aResponse().withStatus(200)));

    try (CloseableHttpResponse r = client.execute(
        new BasicClassicHttpRequest("GET", server.baseUrl() + "/ok"))) {
      // consume response
    }

    assertTrue(AgentTelemetryStore.getInstance().getAll().isEmpty());
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    try (CloseableHttpResponse r = client.execute(
        new BasicClassicHttpRequest("GET", server.baseUrl() + "/not-found"))) {
      // consume response
    }

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<String, Object> json = events.get(0).asJson();
    assertEquals("network", json.get("type"));
    Map<?, ?> body = (Map<?, ?>) json.get("body");
    assertEquals("404", body.get("status_code"));
    assertEquals("GET", body.get("method"));
    String url = body.get("url").toString();
    assertTrue(url.startsWith("http://"), "URL should include scheme: " + url);
    assertTrue(url.contains("localhost"), "URL should include host: " + url);
    assertTrue(url.contains("/not-found"), "URL should include path: " + url);
  }

  @Test
  public void serverErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(post(urlEqualTo("/error")).willReturn(aResponse().withStatus(500)));

    try (CloseableHttpResponse r = client.execute(
        new BasicClassicHttpRequest("POST", server.baseUrl() + "/error"))) {
      // consume response
    }

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<?, ?> body = (Map<?, ?>) events.get(0).asJson().get("body");
    assertEquals("500", body.get("status_code"));
    assertEquals("POST", body.get("method"));
  }

  @Test
  public void responseHandlerOverload_doesNotBreakInstrumentation() throws Exception {
    // execute(ClassicHttpRequest, HttpClientResponseHandler) routes through a separate call chain
    // (HttpHost-based) that does not pass through the instrumented execute(ClassicHttpRequest)
    // or execute(ClassicHttpRequest, HttpContext) overloads. No telemetry event is produced,
    // but the call must succeed without error.
    server.stubFor(get(urlEqualTo("/handler")).willReturn(aResponse().withStatus(404)));

    HttpClientResponseHandler<Integer> handler = response -> response.getCode();
    int status = client.execute(
        new BasicClassicHttpRequest("GET", server.baseUrl() + "/handler"), handler);

    assertEquals(404, status);
    assertTrue(AgentTelemetryStore.getInstance().getAll().isEmpty());
  }

  @Test
  public void urlSanitization_stripsQuery() throws Exception {
    server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

    try (CloseableHttpResponse r = client.execute(
        new BasicClassicHttpRequest("GET", server.baseUrl() + "/path?token=secret"))) {
      // consume response
    }

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<?, ?> body = (Map<?, ?>) events.get(0).asJson().get("body");
    String url = body.get("url").toString();
    assertTrue(url.startsWith("http://"), "URL should include scheme: " + url);
    assertTrue(url.contains("localhost"), "URL should include host: " + url);
    assertTrue(url.contains("/path"), "URL should include path: " + url);
    assertFalse(url.contains("secret"), "URL should not contain query params: " + url);
  }
}
