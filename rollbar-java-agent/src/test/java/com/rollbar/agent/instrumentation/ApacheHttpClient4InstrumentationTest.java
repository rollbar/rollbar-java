package com.rollbar.agent.instrumentation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rollbar.agent.AgentTelemetryStore;
import com.rollbar.agent.NetworkEventBridge;
import org.apache.http.HttpHost;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class ApacheHttpClient4InstrumentationTest {

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

    client.execute(new HttpGet(server.baseUrl() + "/ok")).close();

    assertTrue(AgentTelemetryStore.getAll().isEmpty());
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/not-found")).willReturn(aResponse().withStatus(404)));

    client.execute(new HttpGet(server.baseUrl() + "/not-found")).close();

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

    client.execute(new HttpPost(server.baseUrl() + "/error")).close();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("500", event.get("status_code"));
    assertEquals("POST", event.get("method"));
  }

  @Test
  public void responseHandlerOverload_recordsNetworkEvent() throws Exception {
    // execute(HttpUriRequest, ResponseHandler) routes through execute(HttpHost, HttpRequest, ...),
    // bypassing the single-request overloads. Instrumenting doExecute() — which every dispatch
    // path converges on — is what makes this path visible.
    server.stubFor(get(urlEqualTo("/handler")).willReturn(aResponse().withStatus(404)));

    ResponseHandler<Integer> handler = response -> response.getStatusLine().getStatusCode();
    int status = client.execute(new HttpGet(server.baseUrl() + "/handler"), handler);

    assertEquals(404, status);
    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("404", event.get("status_code"));
    assertTrue(event.get("url").endsWith("/handler"));
  }

  @Test
  public void hostBasedOverload_recordsNetworkEventWithFullUrl() throws Exception {
    // execute(HttpHost, HttpRequest) invokes doExecute() directly. The request carries only a path,
    // so the host must be rejoined from the HttpHost argument for the URL to be usable.
    server.stubFor(get(urlEqualTo("/charge")).willReturn(aResponse().withStatus(500)));

    HttpHost target = new HttpHost("localhost", server.port(), "http");
    client.execute(target, new BasicHttpRequest("GET", "/charge")).close();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("500", event.get("status_code"));
    assertEquals(server.baseUrl() + "/charge", event.get("url"));
  }

  @Test
  public void hostBasedOverloadWithContext_recordsNetworkEvent() throws Exception {
    server.stubFor(get(urlEqualTo("/charge")).willReturn(aResponse().withStatus(503)));

    HttpHost target = new HttpHost("localhost", server.port(), "http");
    client.execute(target, new BasicHttpRequest("GET", "/charge"), new BasicHttpContext()).close();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    assertEquals("503", event.get("status_code"));
    assertEquals(server.baseUrl() + "/charge", event.get("url"));
  }

  @Test
  public void urlSanitization_stripsQuery() throws Exception {
    server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

    client.execute(new HttpGet(server.baseUrl() + "/path?token=secret")).close();

    List<Map<String, String>> events = AgentTelemetryStore.getAll();
    assertEquals(1, events.size());
    Map<String, String> event = events.get(0);
    String url = event.get("url");
    assertTrue(url.contains("/path"));
    assertFalse(url.contains("secret"));
  }

  /**
   * ByteBuddy resolves an advice method's parameter and return types in the classloader that loaded
   * the advice class — the agent's. Naming an {@code org.apache.http} type here throws
   * NoClassDefFoundError at weave time wherever HC 4.x lives in a classloader the agent cannot see
   * (Spring Boot executable jars, per-WAR container classloaders, OSGi), silently disabling this
   * instrumentation. A flat test classpath cannot reproduce that, so pin the signature instead.
   */
  @Test
  public void adviceSignature_namesNoApacheTypes() {
    for (Method method
        : ApacheHttpClient4Instrumentation.DoExecuteAdvice.class.getDeclaredMethods()) {
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
