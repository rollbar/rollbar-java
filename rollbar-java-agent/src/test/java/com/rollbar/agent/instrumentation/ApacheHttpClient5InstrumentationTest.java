package com.rollbar.agent.instrumentation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.NetworkEventBridge;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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
    AgentTelemetryStore.resetForTesting();
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

    assertTrue(AgentTelemetryStore.getAll().isEmpty());
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    try (CloseableHttpResponse r = client.execute(
        new BasicClassicHttpRequest("GET", server.baseUrl() + "/not-found"))) {
      // consume response
    }

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("network", event.get("type"));
    assertEquals("404", event.get("status_code"));
    assertEquals("GET", event.get("method"));
    String url = event.get("url");
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

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("500", event.get("status_code"));
    assertEquals("POST", event.get("method"));
  }

  @Test
  public void responseHandlerOverload_recordsNetworkEvent() throws Exception {
    // execute(ClassicHttpRequest, HttpClientResponseHandler) routes through the HttpHost-based
    // chain, bypassing the single-request overloads. Instrumenting doExecute() — which every
    // dispatch path converges on — is what makes this path visible.
    server.stubFor(get(urlEqualTo("/handler")).willReturn(aResponse().withStatus(404)));

    HttpClientResponseHandler<Integer> handler = response -> response.getCode();
    int status = client.execute(
        new BasicClassicHttpRequest("GET", server.baseUrl() + "/handler"), handler);

    assertEquals(404, status);
    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("404", event.get("status_code").toString());
    assertTrue(event.get("url").endsWith("/handler"));
  }

  @Test
  public void hostBasedOverload_recordsNetworkEventWithFullUrl() throws Exception {
    // execute(HttpHost, ClassicHttpRequest) invokes doExecute() directly. The request carries only
    // a path, so the host must be rejoined from the HttpHost argument for the URL to be usable.
    server.stubFor(get(urlEqualTo("/charge")).willReturn(aResponse().withStatus(500)));

    HttpHost target = new HttpHost("http", "localhost", server.port());
    client.execute(target, new BasicClassicHttpRequest("GET", "/charge")).close();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("500", event.get("status_code").toString());
    assertEquals(server.baseUrl() + "/charge", event.get("url"));
  }

  @Test
  public void hostBasedOverloadWithContext_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/charge")).willReturn(aResponse().withStatus(503)));

    HttpHost target = new HttpHost("http", "localhost", server.port());
    try (CloseableHttpResponse response = client.execute(
        target, new BasicClassicHttpRequest("GET", "/charge"), new BasicHttpContext())) {
      assertEquals(503, response.getCode());
    }

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("503", event.get("status_code").toString());
    assertEquals(server.baseUrl() + "/charge", event.get("url"));
  }

  @Test
  public void urlSanitization_stripsQuery() throws Exception {
    server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

    try (CloseableHttpResponse r = client.execute(
        new BasicClassicHttpRequest("GET", server.baseUrl() + "/path?token=secret"))) {
      // consume response
    }

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    String url = event.get("url");
    assertTrue(url.startsWith("http://"), "URL should include scheme: " + url);
    assertTrue(url.contains("localhost"), "URL should include host: " + url);
    assertTrue(url.contains("/path"), "URL should include path: " + url);
    assertFalse(url.contains("secret"), "URL should not contain query params: " + url);
  }

  /**
   * ByteBuddy resolves an advice method's parameter and return types in the classloader that loaded
   * the advice class — the agent's. Naming an {@code org.apache.hc} type here throws
   * NoClassDefFoundError at weave time wherever HC 5.x lives in a classloader the agent cannot see
   * (Spring Boot executable jars, per-WAR container classloaders, OSGi), silently disabling this
   * instrumentation. A flat test classpath cannot reproduce that, so pin the signature instead.
   */
  @Test
  public void adviceSignature_namesNoApacheTypes() {
    for (Method method
        : ApacheHttpClient5Instrumentation.DoExecuteAdvice.class.getDeclaredMethods()) {
      if (method.isSynthetic()) {
        continue; // e.g. JaCoCo's $jacocoInit(), which ByteBuddy never resolves as advice
      }
      for (Class<?> parameterType : method.getParameterTypes()) {
        assertTrue(isAgentVisible(parameterType),
            "advice parameter type must be resolvable from the agent's classloader: "
                + parameterType.getName());
      }
      assertTrue(isAgentVisible(method.getReturnType()),
          "advice return type must be resolvable from the agent's classloader: "
              + method.getReturnType().getName());
    }
  }

  private static boolean isAgentVisible(Class<?> type) {
    String name = type.getName();
    return type.isPrimitive() || name.startsWith("java.") || name.startsWith("com.rollbar.");
  }
}
