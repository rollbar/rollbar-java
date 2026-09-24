package com.rollbar.agent.instrumentation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.NetworkEventBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class HttpUrlConnectionInstrumentationTest {

  private WireMockServer server;

  @BeforeEach
  public void setUp() {
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    AgentTelemetryStore.resetForTesting();
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

    assertTrue(AgentTelemetryStore.getAll().isEmpty());
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    makeRequest("GET", "/not-found");

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("network", event.get("type"));
    assertEquals("404", event.get("status_code"));
    assertEquals("GET", event.get("method"));
    assertTrue(event.get("url").contains("/not-found"));
  }

  @Test
  public void serverErrorResponse_recordsNetworkEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/error")).willReturn(aResponse().withStatus(500)));

    makeRequest("GET", "/error");

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("500", event.get("status_code"));
  }

  @Test
  public void redirectResponse_doesNotRecordEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/redirect")).willReturn(
        aResponse().withStatus(301).withHeader("Location", "/other")));

    HttpURLConnection conn = (HttpURLConnection) new URL(server.baseUrl() + "/redirect").openConnection();
    conn.setInstanceFollowRedirects(false);
    conn.getResponseCode();
    conn.disconnect();

    assertTrue(AgentTelemetryStore.getAll().isEmpty());
  }

  @Test
  public void urlSanitization_stripsQueryAndCredentials() throws IOException {
    server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

    HttpURLConnection conn = (HttpURLConnection) new URL(
        server.baseUrl() + "/path?secret=abc&token=xyz"
    ).openConnection();
    conn.getResponseCode();
    conn.disconnect();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    String url = event.get("url");
    assertTrue(url.contains("/path"));
    assertFalse(url.contains("secret"));
    assertFalse(url.contains("token"));
  }

  @Test
  public void getInputStream_on4xx_recordsNetworkEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    HttpURLConnection conn = (HttpURLConnection) new URL(server.baseUrl() + "/not-found")
        .openConnection();
    conn.setRequestMethod("GET");
    try {
      conn.getInputStream();
    } catch (IOException ignored) {
      // expected for 4xx
    }
    conn.disconnect();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("404", event.get("status_code"));
    assertEquals("GET", event.get("method"));
  }

  @Test
  public void getInputStream_on2xx_doesNotRecordEvent() throws IOException {
    server.stubFor(get(urlEqualTo("/ok")).willReturn(aResponse().withStatus(200)));

    HttpURLConnection conn = (HttpURLConnection) new URL(server.baseUrl() + "/ok")
        .openConnection();
    conn.setRequestMethod("GET");
    conn.getInputStream().close();
    conn.disconnect();

    assertTrue(AgentTelemetryStore.getAll().isEmpty());
  }

  @Test
  public void getInputStream_thenGetErrorStream_doesNotDoubleRecord() throws IOException {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    HttpURLConnection conn = (HttpURLConnection) new URL(server.baseUrl() + "/not-found")
        .openConnection();
    conn.setRequestMethod("GET");
    try {
      conn.getInputStream();
    } catch (IOException ignored) {
      // expected for 4xx
    }
    conn.getErrorStream();
    conn.disconnect();

    assertEquals(1, AgentTelemetryStore.getAll().size());
  }

  @Test
  public void getInputStream_connectionRefused_recordsErrorWithoutRecursion() throws IOException {
    // A connection-level failure leaves responseCode == -1, so the JDK's getResponseCode() calls
    // getInputStream() again. Before the re-entry guard this recursed until a StackOverflowError
    // that the advice swallowed, recording nothing. Now it must record a single error event and
    // return promptly.
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    } // socket closed here → connections to closedPort are refused

    HttpURLConnection conn = (HttpURLConnection) new URL(
        "http://127.0.0.1:" + closedPort + "/x").openConnection();
    conn.setConnectTimeout(1000);
    conn.setReadTimeout(1000);
    try {
      conn.getInputStream();
      fail("expected connection to be refused");
    } catch (IOException expected) {
      // expected: nothing is listening on the port
    }
    conn.disconnect();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size(), "connection failure should record exactly one event");
    Map<String, String> event = events.get(0);
    assertEquals("manual", event.get("type"));
    assertTrue(event.get("message").contains("Network error"),
        "error event should carry a network-error message");
  }

  @Test
  public void getResponseCode_connectionRefused_recordsSingleError() throws IOException {
    // The other entry point into the same failure. Deduplicating on the thrown exception recorded
    // three events here: responseCode stays -1, so the JDK retries getInputStream() internally and
    // builds a fresh exception object per attempt, which an identity-keyed set cannot collapse.
    // The connection instance is the one identity that is stable across those retries.
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    } // socket closed here → connections to closedPort are refused

    HttpURLConnection conn = (HttpURLConnection) new URL(
        "http://127.0.0.1:" + closedPort + "/x").openConnection();
    conn.setConnectTimeout(1000);
    conn.setReadTimeout(1000);
    try {
      conn.getResponseCode();
      fail("expected connection to be refused");
    } catch (IOException expected) {
      // expected: nothing is listening on the port
    }
    conn.disconnect();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size(), "connection failure should record exactly one event");
    Map<String, String> event = events.get(0);
    assertEquals("manual", event.get("type"));
    assertTrue(event.get("message").contains("Network error"),
        "error event should carry a network-error message");
  }

  private void makeRequest(String method, String path) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(server.baseUrl() + path).openConnection();
    conn.setRequestMethod(method);
    conn.getResponseCode();
    conn.disconnect();
  }
}
