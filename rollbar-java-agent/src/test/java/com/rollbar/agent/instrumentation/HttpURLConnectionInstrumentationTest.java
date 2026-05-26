package com.rollbar.agent.instrumentation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.NetworkEventBridge;
import com.rollbar.api.payload.data.TelemetryEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class HttpURLConnectionInstrumentationTest {

  private WireMockServer server;

  @BeforeEach
  public void setUp() {
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    AgentTelemetryStore.init(System::currentTimeMillis);
    NetworkEventBridge.resetRecordedForTesting();
  }

  @AfterEach
  public void tearDown() {
    server.stop();
  }

  @Test
  public void successResponse_doesNotRecordEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/ok")).willReturn(aResponse().withStatus(200)));

    makeRequest("GET", "/ok");

    assertTrue(AgentTelemetryStore.getInstance().getAll().isEmpty());
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    makeRequest("GET", "/not-found");

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<String, Object> json = events.get(0).asJson();
    assertEquals("network", json.get("type"));
    Map<?, ?> body = (Map<?, ?>) json.get("body");
    assertEquals("404", body.get("status_code"));
    assertEquals("GET", body.get("method"));
    assertTrue(body.get("url").toString().contains("/not-found"));
  }

  @Test
  public void serverErrorResponse_recordsNetworkEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/error")).willReturn(aResponse().withStatus(500)));

    makeRequest("GET", "/error");

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<?, ?> body = (Map<?, ?>) events.get(0).asJson().get("body");
    assertEquals("500", body.get("status_code"));
  }

  @Test
  public void redirectResponse_doesNotRecordEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/redirect")).willReturn(
        aResponse().withStatus(301).withHeader("Location", "/other")));

    HttpURLConnection conn = (HttpURLConnection) new URL(server.baseUrl() + "/redirect").openConnection();
    conn.setInstanceFollowRedirects(false);
    conn.getResponseCode();
    conn.disconnect();

    assertTrue(AgentTelemetryStore.getInstance().getAll().isEmpty());
  }

  @Test
  public void urlSanitization_stripsQueryAndCredentials() throws IOException {
    server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

    HttpURLConnection conn = (HttpURLConnection) new URL(
        server.baseUrl() + "/path?secret=abc&token=xyz"
    ).openConnection();
    conn.getResponseCode();
    conn.disconnect();

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<?, ?> body = (Map<?, ?>) events.get(0).asJson().get("body");
    String url = body.get("url").toString();
    assertTrue(url.contains("/path"));
    assertFalse(url.contains("secret"));
    assertFalse(url.contains("token"));
  }

  private void makeRequest(String method, String path) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(server.baseUrl() + path).openConnection();
    conn.setRequestMethod(method);
    conn.getResponseCode();
    conn.disconnect();
  }
}
