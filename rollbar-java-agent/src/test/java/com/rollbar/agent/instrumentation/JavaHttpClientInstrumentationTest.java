package com.rollbar.agent.instrumentation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.NetworkEventBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
    AgentTelemetryStore.resetForTesting();
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

    assertTrue(AgentTelemetryStore.getAll().isEmpty());
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    client.send(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/not-found")).GET().build(),
        HttpResponse.BodyHandlers.discarding()
    );

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("network", event.get("type"));
    assertEquals("404", event.get("status_code"));
    assertEquals("GET", event.get("method"));
    assertTrue(event.get("url").contains("/not-found"));
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

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("500", event.get("status_code"));
    assertEquals("POST", event.get("method"));
  }

  @Test
  public void sendAsync_successResponse_doesNotRecordEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/ok-async")).willReturn(aResponse().withStatus(200)));

    client.sendAsync(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/ok-async")).GET().build(),
        HttpResponse.BodyHandlers.discarding()
    ).get(5, TimeUnit.SECONDS);

    // whenComplete callbacks fire in the HTTP thread; no event expected for 2xx
    Thread.sleep(50);
    assertTrue(AgentTelemetryStore.getAll().isEmpty());
  }

  @Test
  public void sendAsync_clientErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/not-found-async")).willReturn(aResponse().withStatus(404)));

    client.sendAsync(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/not-found-async")).GET().build(),
        HttpResponse.BodyHandlers.discarding()
    ).get(5, TimeUnit.SECONDS);

    List<Map<String, String>> events = awaitEvents(AgentTelemetryStore::getAll);
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("network", event.get("type"));
    assertEquals("404", event.get("status_code"));
    assertEquals("GET", event.get("method"));
    assertTrue(event.get("url").contains("/not-found-async"));
  }

  @Test
  public void sendAsync_serverErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(post(urlEqualTo("/error-async")).willReturn(aResponse().withStatus(500)));

    client.sendAsync(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/error-async"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build(),
        HttpResponse.BodyHandlers.discarding()
    ).get(5, TimeUnit.SECONDS);

    List<Map<String, String>> events = awaitEvents(AgentTelemetryStore::getAll);
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("500", event.get("status_code"));
    assertEquals("POST", event.get("method"));
  }

  /**
   * Polls until the supplier returns a list with at least {@code minCount} elements or
   * {@code timeoutMs} elapses. The {@code whenComplete} callbacks from async advice fire in
   * the HTTP-client thread, so they may arrive a few milliseconds after {@code get()} returns.
   */
  private static List<Map<String, String>> awaitEvents(
          Supplier<List<Map<String, String>>> supplier)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + (long) 1000;
    List<Map<String, String>> events;
    do {
      events = supplier.get();
      if (!events.isEmpty()) {
        return events;
      }
      Thread.sleep(5);
    } while (System.currentTimeMillis() < deadline);
    return events;
  }

  @Test
  public void urlSanitization_stripsQuery() throws Exception {
    server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

    client.send(
        HttpRequest.newBuilder(URI.create(server.baseUrl() + "/path?token=secret")).GET().build(),
        HttpResponse.BodyHandlers.discarding()
    );

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    String url = event.get("url");
    assertTrue(url.contains("/path"));
    assertFalse(url.contains("secret"));
  }
}
