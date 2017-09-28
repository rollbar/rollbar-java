package com.rollbar.notifier.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.fingerprint.FingerprintGenerator;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.Sender;
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
  public void shouldBuildTheConfiguration() {
    Config config = new ConfigBuilder()
        .accessToken(ACCESS_TOKEN)
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
  }
}