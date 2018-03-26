package com.rollbar.notifier.config;

import static com.rollbar.notifier.config.ConfigBuilder.withConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.fingerprint.FingerprintGenerator;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.provider.notifier.NotifierProvider;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.notifier.uuid.UuidGenerator;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
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

  @Test
  public void shouldBuildConfigurationWithDefaults() {
    Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN).build();

    assertThat(config.language(), is("java"));
    assertThat(config.notifier(), is(instanceOf(NotifierProvider.class)));
    assertThat(config.sender(), is(instanceOf(SyncSender.class)));
    assertThat(config.handleUncaughtErrors(), is(true));
    assertThat(config.isEnabled(), is(true));
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
    assertThat(config.handleUncaughtErrors(), is(false));
    assertThat(config.isEnabled(), is(false));
  }

  @Test
  public void shouldBuildWithConfig() {
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
            .handleUncaughtErrors(false)
            .enabled(false)
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
    assertThat(config.handleUncaughtErrors(), is(copy.handleUncaughtErrors()));
    assertThat(config.isEnabled(), is(copy.isEnabled()));
  }

  @Test
  public void test() {
    Config config = ConfigBuilder.withAccessToken("mi token")
        .build();

    Sender sender = config.sender();

    System.out.println(sender);
  }
}