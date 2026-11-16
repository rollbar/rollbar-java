package com.rollbar.notifier.config;

import static com.rollbar.notifier.config.ConfigBuilder.withConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.fingerprint.FingerprintGenerator;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.provider.notifier.NotifierProvider;
import com.rollbar.notifier.sender.BufferedSender;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.notifier.uuid.UuidGenerator;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ConfigBuilderTest {

  static final String ACCESS_TOKEN = "rollbar_access_token";

  static final String ENVIRONMENT = "environment";

  static final String CODE_VERSION = "code_version";

  static final String PLATFORM = "platform";

  static final String LANGUAGE = "language";

  static final String FRAMEWORK = "framework";

  private static final int DEFAULT_CAPACITY_FOR_TELEMETRY_EVENTS = 100;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  Provider<String> context;

  @Mock
  Provider<Request> request;

  @Mock
  Provider<Person> person;

  @Mock
  Provider<Server> server;

  @Mock
  Provider<Client> client;

  @Mock
  Provider<Map<String, Object>> custom;

  @Mock
  Provider<Notifier> notifier;

  @Mock
  Filter filter;

  @Mock
  Transformer transformer;

  @Mock
  FingerprintGenerator fingerPrintGenerator;

  @Mock
  UuidGenerator uuidGenerator;

  @Mock
  Sender sender;

  @Mock
  Proxy proxy;

  @Test
  public void shouldBuildConfigurationWithDefaults() {
    Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN).build();

    assertThat(config.language(), is("java"));
    assertThat(config.notifier(), is(instanceOf(NotifierProvider.class)));
    assertThat(config.sender(), is(instanceOf(BufferedSender.class)));
    assertThat(config.handleUncaughtErrors(), is(true));
    assertThat(config.isEnabled(), is(true));
    assertThat(config.proxy(), is(nullValue()));
    assertThat(config.connectTimeoutMillis(), is(SyncSender.DEFAULT_CONNECT_TIMEOUT_MILLIS));
    assertThat(config.readTimeoutMillis(), is(SyncSender.DEFAULT_READ_TIMEOUT_MILLIS));

    assertThat(config.defaultMessageLevel(), equalTo(Level.WARNING));
    assertThat(config.defaultThrowableLevel(), equalTo(Level.ERROR));
    assertThat(config.defaultErrorLevel(), equalTo(Level.CRITICAL));
  }

  @Test
  public void shouldBuildTheConfiguration() {
    Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN)
        .environment(ENVIRONMENT)
        .codeVersion(CODE_VERSION)
        .platform(PLATFORM)
        .language(LANGUAGE)
        .framework(FRAMEWORK)
        .context(context)
        .request(request)
        .person(person)
        .server(server)
        .client(client)
        .custom(custom)
        .notifier(notifier)
        .filter(filter)
        .transformer(transformer)
        .fingerPrintGenerator(fingerPrintGenerator)
        .uuidGenerator(uuidGenerator)
        .sender(sender)
        .proxy(proxy)
        .handleUncaughtErrors(false)
        .enabled(false)
        .build();

    assertThat(config.accessToken(), is(ACCESS_TOKEN));
    assertThat(config.environment(), is(ENVIRONMENT));
    assertThat(config.codeVersion(), is(CODE_VERSION));
    assertThat(config.platform(), is(PLATFORM));
    assertThat(config.language(), is(LANGUAGE));
    assertThat(config.framework(), is(FRAMEWORK));
    assertThat(config.context(), is(context));
    assertThat(config.request(), is(request));
    assertThat(config.person(), is(person));
    assertThat(config.server(), is(server));
    assertThat(config.client(), is(client));
    assertThat(config.custom(), is(custom));
    assertThat(config.notifier(), is(notifier));
    assertThat(config.filter(), is(filter));
    assertThat(config.transformer(), is(transformer));
    assertThat(config.fingerPrintGenerator(), is(fingerPrintGenerator));
    assertThat(config.uuidGenerator(), is(uuidGenerator));
    assertThat(config.sender(), is(sender));
    assertThat(config.proxy(), is(proxy));
    assertThat(config.handleUncaughtErrors(), is(false));
    assertThat(config.isEnabled(), is(false));
    assertThat(config.maximumTelemetryData(), is(DEFAULT_CAPACITY_FOR_TELEMETRY_EVENTS));
  }

  @Test
  public void shouldBuildWithConfig() {
    int maximumTelemetryData = 3;
    Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN)
            .environment(ENVIRONMENT)
            .codeVersion(CODE_VERSION)
            .platform(PLATFORM)
            .language(LANGUAGE)
            .framework(FRAMEWORK)
            .context(context)
            .request(request)
            .person(person)
            .server(server)
            .client(client)
            .custom(custom)
            .notifier(notifier)
            .filter(filter)
            .transformer(transformer)
            .fingerPrintGenerator(fingerPrintGenerator)
            .uuidGenerator(uuidGenerator)
            .sender(sender)
            .proxy(proxy)
            .handleUncaughtErrors(false)
            .enabled(false)
            .maximumTelemetryData(maximumTelemetryData)
            .build();

    Config copy = withConfig(config).build();

    assertThat(config.accessToken(), is(copy.accessToken()));
    assertThat(config.environment(), is(copy.environment()));
    assertThat(config.codeVersion(), is(copy.codeVersion()));
    assertThat(config.platform(), is(copy.platform()));
    assertThat(config.language(), is(copy.language()));
    assertThat(config.framework(), is(copy.framework()));
    assertThat(config.context(), is(copy.context()));
    assertThat(config.request(), is(copy.request()));
    assertThat(config.person(), is(copy.person()));
    assertThat(config.server(), is(copy.server()));
    assertThat(config.client(), is(copy.client()));
    assertThat(config.custom(), is(copy.custom()));
    assertThat(config.notifier(), is(copy.notifier()));
    assertThat(config.filter(), is(copy.filter()));
    assertThat(config.transformer(), is(copy.transformer()));
    assertThat(config.fingerPrintGenerator(), is(copy.fingerPrintGenerator()));
    assertThat(config.uuidGenerator(), is(copy.uuidGenerator()));
    assertThat(config.sender(), is(copy.sender()));
    assertThat(config.proxy(), is(copy.proxy()));
    assertThat(config.handleUncaughtErrors(), is(copy.handleUncaughtErrors()));
    assertThat(config.isEnabled(), is(copy.isEnabled()));
    assertThat(config.maximumTelemetryData(), is(maximumTelemetryData));
  }

  @Test
  public void shouldBuildWithTimeoutsAndCopyThem() {
    Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN)
        .connectTimeoutMillis(1234)
        .readTimeoutMillis(5678)
        .build();

    Config copy = withConfig(config).build();

    assertThat(config.connectTimeoutMillis(), is(1234));
    assertThat(config.readTimeoutMillis(), is(5678));
    assertThat(copy.connectTimeoutMillis(), is(1234));
    assertThat(copy.readTimeoutMillis(), is(5678));
  }

  @Test(timeout = 10_000)
  public void shouldApplyReadTimeoutToTheDefaultSender() throws Exception {
    // Accepts the connection (via the backlog) but never responds. If the configured timeout did
    // not reach the sender, it would wait for the 30 s default and this test would time out.
    String host = "127.0.0.1";
    try (ServerSocket silentServer = new ServerSocket(0, 1, InetAddress.getByName(host))) {
      Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN)
          .endpoint("http://" + host + ":" + silentServer.getLocalPort() + "/api/1/item/")
          .readTimeoutMillis(200)
          .build();
      try {
        Sender defaultSender = ((BufferedSender) config.sender()).sender();
        assertThat(defaultSender, is(instanceOf(SyncSender.class)));
        SenderListener listener = mock(SenderListener.class);
        defaultSender.addListener(listener);
        Payload payload = new Payload("{}");

        defaultSender.send(payload);

        ArgumentCaptor<SenderException> error = ArgumentCaptor.forClass(SenderException.class);
        verify(listener).onError(eq(payload), error.capture());
        assertThat(error.getValue().getCause(), is(instanceOf(SocketTimeoutException.class)));
      } finally {
        config.sender().close(false);
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectNegativeConnectTimeout() {
    ConfigBuilder.withAccessToken(ACCESS_TOKEN).connectTimeoutMillis(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectNegativeReadTimeout() {
    ConfigBuilder.withAccessToken(ACCESS_TOKEN).readTimeoutMillis(-1);
  }
}