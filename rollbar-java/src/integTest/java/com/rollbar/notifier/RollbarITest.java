package com.rollbar.notifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;
import static java.lang.String.format;
import static org.junit.Assert.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.trafficlistener.ConsoleNotifyingWiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Message;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.util.ConstantTimeProvider;
import com.rollbar.notifier.util.RollbarResponse;
import com.rollbar.notifier.util.SenderAssertions;
import com.rollbar.notifier.util.json.BodySerializer;
import com.rollbar.notifier.util.json.DataSerializer;
import com.rollbar.notifier.util.json.LevelSerializer;
import com.rollbar.notifier.util.json.PayloadSerializer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RollbarITest {
  private static final String PROXY_HOST = "localhost";

  private static final String ACCESS_TOKEN = UUID.randomUUID().toString();

  private static final String ERROR_MESSAGE = "This is an error message.";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  private Gson gson;

  private ConfigBuilder configBuilder;

  private Provider<Long> timeProvider;

  private Sender sender;

  @Before
  public void setUp() {
    LevelSerializer levelSerializer = new LevelSerializer();
    BodySerializer bodySerializer = new BodySerializer();
    PayloadSerializer payloadSerializer = new PayloadSerializer();
    DataSerializer dataSerializer = new DataSerializer();

    this.gson = new GsonBuilder()
        .registerTypeAdapter(Level.class, levelSerializer)
        .registerTypeAdapter(Body.class, bodySerializer)
        .registerTypeAdapter(Payload.class, payloadSerializer)
        .registerTypeAdapter(Data.class, dataSerializer)
        .create();

    this.timeProvider = new ConstantTimeProvider();

    this.sender = buildSender(getUrl(), ACCESS_TOKEN, null);

    this.configBuilder = withAccessToken(ACCESS_TOKEN)
        .sender(sender)
        .timestamp(timeProvider);
  }

  @Test
  public void shouldSendSuccessfullyMessagePayload() {
    final String uuid = UUID.randomUUID().toString();

    Config config = configBuilder.build();

    Message message = new Message.Builder()
        .body(ERROR_MESSAGE)
        .build();

    Body body = new Body.Builder()
        .bodyContent(message)
        .build();

    Data data = new Data.Builder()
        .level(Level.ERROR)
        .body(body)
        .notifier(config.notifier().provide())
        .timestamp(timeProvider.provide())
        .language(config.language())
        .build();

    Payload payload = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(data)
        .build();

    stubFor(post(urlEqualTo("/api/1/item/"))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Accept-Charset", equalTo("UTF-8"))
        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
        .withHeader("x-rollbar-access-token", equalTo(ACCESS_TOKEN))
        .withRequestBody(equalToJson(gson.toJson(payload)))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(RollbarResponse.success(uuid)))));

    SenderAssertions.SuccessAssertion listener =
        SenderAssertions.assertResponseSuccess(uuid);

    sender.addListener(listener);

    Rollbar rollbar = new Rollbar(config);

    rollbar.error(ERROR_MESSAGE);

    listener.assertCalled();
  }

  @Test
  public void shouldSendSuccessfullyMessagePayloadThroughProxy() throws Exception {
    ProxyServer proxyServer = buildProxyServer();
    proxyServer.start();

    try {
      int proxyPort = proxyServer.getPort();

      Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(PROXY_HOST, proxyPort));

      String url = getUrl();

      Sender sender = buildSender(url, ACCESS_TOKEN, proxy);

      ConfigBuilder configBuilder = withAccessToken(ACCESS_TOKEN)
          .sender(sender)
          .timestamp(timeProvider);

      final String uuid = UUID.randomUUID().toString();

      Config config = configBuilder.build();

      Message message = new Message.Builder()
          .body(ERROR_MESSAGE)
          .build();

      Body body = new Body.Builder()
          .bodyContent(message)
          .build();

      Data data = new Data.Builder()
          .level(Level.ERROR)
          .body(body)
          .notifier(config.notifier().provide())
          .timestamp(timeProvider.provide())
          .language(config.language())
          .build();

      Payload payload = new Payload.Builder()
          .accessToken(ACCESS_TOKEN)
          .data(data)
          .build();

      stubFor(post(urlEqualTo("/api/1/item/"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Accept-Charset", equalTo("UTF-8"))
          .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
          .withHeader("x-rollbar-access-token", equalTo(ACCESS_TOKEN))
          .withRequestBody(equalToJson(gson.toJson(payload)))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(gson.toJson(RollbarResponse.success(uuid)))));

      SenderAssertions.SuccessAssertion listener =
          SenderAssertions.assertResponseSuccess(uuid);
      sender.addListener(listener);

      Rollbar rollbar = new Rollbar(config);

      rollbar.error(ERROR_MESSAGE);

      listener.assertCalled();

      WireMock.verify(postRequestedFor(urlEqualTo("/api/1/item/"))
          .withHeader("Content-Type", equalTo("application/json; charset=UTF-8")));

      assertThat(proxyServer.requestCount(), Matchers.equalTo(1));
    } finally {
      proxyServer.stop();
    }
  }

  @Test
  public void shouldNotifyAccessTokenRequiredError() {
    int responseCode = 400;
    String errorMessage = "access token required";

    Sender sender = buildSender(getUrl(), null, null);

    Config config = configBuilder
        .accessToken(null)
        .sender(sender)
        .build();

    Message message = new Message.Builder()
        .body(ERROR_MESSAGE)
        .build();

    Body body = new Body.Builder()
        .bodyContent(message)
        .build();

    Data data = new Data.Builder()
        .level(Level.ERROR)
        .body(body)
        .notifier(config.notifier().provide())
        .timestamp(timeProvider.provide())
        .language(config.language())
        .build();

    Payload payload = new Payload.Builder()
        .data(data)
        .build();

    stubFor(post(urlEqualTo("/api/1/item/"))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Accept-Charset", equalTo("UTF-8"))
        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
        .withRequestBody(equalToJson(gson.toJson(payload)))
        .willReturn(aResponse()
            .withStatus(responseCode)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(RollbarResponse.error(errorMessage)))));

    SenderAssertions.ErrorAssertion
        listener = SenderAssertions.assertApiError(responseCode, errorMessage);
    sender.addListener(listener);

    Rollbar rollbar = new Rollbar(config);

    rollbar.error(ERROR_MESSAGE);

    listener.assertCalled();
  }

  @Test
  public void shouldNotifyInvalidResponseError() {
    Sender sender = buildSender(getUrl(), null, null);

    Config config = configBuilder
        .accessToken(null)
        .sender(sender)
        .build();

    stubFor(post(urlEqualTo("/api/1/item/"))
        .willReturn(aResponse()
            .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

    SenderAssertions.ErrorAssertion
        listener = SenderAssertions.assertSenderError(IOException.class);
    sender.addListener(listener);

    Rollbar rollbar = new Rollbar(config);

    rollbar.error(ERROR_MESSAGE);

    listener.assertCalled();
  }

  protected Sender buildSender(String url, String accessToken, Proxy proxy) {
    SyncSender.Builder builder = new SyncSender.Builder()
        .url(url);

    if (accessToken != null) {
      builder = builder.accessToken(accessToken);
    }

    if (proxy != null) {
      builder = builder.proxy(proxy);
    }

    return builder.build();
  }

  private int getPort() {
    return wireMockRule.port();
  }

  private String getUrl() {
    return format(Locale.US, "http://localhost:%d/api/1/item/", getPort());
  }

  public static class BlockingListener implements SenderListener {
    public AtomicBoolean done = new AtomicBoolean(false);
    private final long timeout;

    public BlockingListener(long timeoutMs) {
      this.timeout = timeoutMs;
    }

    @Override
    public void onResponse(Payload payload, Response response) {
      done.set(true);
    }

    @Override
    public void onError(Payload payload, Exception error) {
      done.set(true);
    }

    public void clear() {
      done.set(false);
    }

    public void block() throws InterruptedException, TimeoutException {
      long remaining = timeout;
      int sleepTime = 20;

      while (!done.get()) {
        if (remaining < 0) {
          throw new TimeoutException("Timed out waiting for result");
        }

        Thread.sleep(sleepTime);
        remaining -= sleepTime;
      }
    }
  }

  private ProxyServer buildProxyServer() {
    // WireMock only supports 'CONNECT' for SSL requests, and injects some SSL data into the stream
    // automatically after a request, which fails in our case since we're not actually using SSL.
    // Netty's HTTP client always uses 'CONNECT' for proxying, even for HTTP, so we can't use
    // WireMock as the proxy in the Reactor tests.
    // See com.github.tomakehurst.wiremock.jetty94.ManInTheMiddleSslConnectHandler

    HandlerCollection handlers = new HandlerCollection();
    RequestCounter requestCounter = new RequestCounter();
    handlers.addHandler(requestCounter);

    ConnectHandler proxy = new ConnectHandler();

    ServletContextHandler context = new ServletContextHandler(proxy, "/",
        ServletContextHandler.SESSIONS);
    ServletHolder proxyServlet = new ServletHolder(ProxyServlet.class);
    context.addServlet(proxyServlet, "/*");

    handlers.addHandler(proxy);

    Server server = new Server();
    server.setHandler(handlers);

    ServerConnector connector = new ServerConnector(server);
    connector.setPort(0);
    server.addConnector(connector);

    return new ProxyServer(server, requestCounter);
  }

  private static class ProxyServer {
    private final Server proxyServer;
    private final RequestCounter reqCounter;

    public ProxyServer(Server proxyServer, RequestCounter counter) {
      this.proxyServer = proxyServer;
      this.reqCounter = counter;
    }

    public int requestCount() {
      return reqCounter.counter.get();
    }

    public void start() throws Exception {
      proxyServer.start();
    }

    public void stop() throws Exception {
      proxyServer.stop();
    }

    public int getPort() {
      return ((ServerConnector) proxyServer.getConnectors()[0]).getLocalPort();
    }
  }

  private static class RequestCounter extends AbstractHandler {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) {
      counter.incrementAndGet();
    }
  }
}
