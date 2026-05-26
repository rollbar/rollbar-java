package com.rollbar.agent.instrumentation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.NetworkEventBridge;
import com.rollbar.api.payload.data.TelemetryEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class JavaHttpClientInstrumentationTest {

  private WireMockServer server;
  private HttpClient client;

  @BeforeEach
  public void setUp() {
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    client = HttpClient.newHttpClient();
    AgentTelemetryStore.init(System::currentTimeMillis);
    NetworkEventBridge.resetRecordedForTesting();
  }

  @AfterEach
  public void tearDown() {
    server.stop();
  }

  @Test
  public void successResponse_doesNotRecordEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/ok")).willReturn(aResponse().withStatus(200)));

    client.send(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/ok")).GET().build(),
        HttpResponse.BodyHandlers.discarding()
    );

    assertTrue(AgentTelemetryStore.getInstance().getAll().isEmpty());
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    client.send(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/not-found")).GET().build(),
        HttpResponse.BodyHandlers.discarding()
    );

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
  public void serverErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(post(urlEqualTo("/error")).willReturn(aResponse().withStatus(500)));

    client.send(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/error"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build(),
        HttpResponse.BodyHandlers.discarding()
    );

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<?, ?> body = (Map<?, ?>) events.get(0).asJson().get("body");
    assertEquals("500", body.get("status_code"));
    assertEquals("POST", body.get("method"));
  }

  @Test
  public void urlSanitization_stripsQuery() throws Exception {
    server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

    client.send(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/path?token=secret")).GET().build(),
        HttpResponse.BodyHandlers.discarding()
    );

    List<TelemetryEvent> events = AgentTelemetryStore.getInstance().getAll();
    assertEquals(1, events.size());
    Map<?, ?> body = (Map<?, ?>) events.get(0).asJson().get("body");
    String url = body.get("url").toString();
    assertTrue(url.contains("/path"));
    assertFalse(url.contains("secret"));
  }
}
