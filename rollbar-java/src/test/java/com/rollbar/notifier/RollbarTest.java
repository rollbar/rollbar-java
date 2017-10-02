package com.rollbar.notifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Data.Builder;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.fingerprint.FingerprintGenerator;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.notifier.uuid.UuidGenerator;
import java.awt.image.DataBuffer;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RollbarTest {

  static final String ACCESS_TOKEN = "access_token";

  static final String ENVIRONMENT = "environment";

  static final String CODE_VERSION = "code_version";

  static final String PLATFORM = "platform";

  static final String LANGUAGE = "language";

  static final String FRAMEWORK = "framework";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  Provider<String> contextProvider;

  @Mock
  Provider<Request> requestProvider;

  @Mock
  Provider<Person> personProvider;

  @Mock
  Provider<Server> serverProvider;

  @Mock
  Provider<Client> clientProvider;

  @Mock
  Provider<Map<String, Object>> customProvider;

  @Mock
  Provider<Notifier> notifierProvider;

  @Mock
  Provider<Long> timestampProvider;

  @Mock
  Filter filter;

  @Mock
  Transformer transformer;

  @Mock
  FingerprintGenerator fingerprintGenerator;

  @Mock
  UuidGenerator uuidGenerator;

  @Mock
  Sender sender;

  @Mock
  BodyFactory bodyFactory;

  Config config;

  @Before
  public void setUp() {
    config = new ConfigBuilder()
        .accessToken(ACCESS_TOKEN)
        .environment(ENVIRONMENT)
        .codeVersion(CODE_VERSION)
        .platform(PLATFORM)
        .language(LANGUAGE)
        .framework(FRAMEWORK)
        .context(contextProvider)
        .request(requestProvider)
        .person(personProvider)
        .server(serverProvider)
        .client(clientProvider)
        .custom(customProvider)
        .notifier(notifierProvider)
        .timestamp(timestampProvider)
        .filter(filter)
        .transformer(transformer)
        .fingerPrintGenerator(fingerprintGenerator)
        .uuidGenerator(uuidGenerator)
        .sender(sender)
        .build();
  }

  @Test
  public void shouldProcessAllInformation() {
    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    Body body = mock(Body.class);

    String context = "context";
    Request request = mock(Request.class);
    Person person = mock(Person.class);
    Server server = mock(Server.class);
    Client client = mock(Client.class);
    Map<String, Object> custom = new HashMap<>();
    custom.put("custom1", "value1");
    Map<String, Object> extraCustom = new HashMap<>();
    extraCustom.put("custom2", "value2");
    Notifier notifier = mock(Notifier.class);
    Long timestamp = System.currentTimeMillis();
    String fingerprint = "fingerprint";
    String uuid = "uuid";
    String description = "description";

    Map<String, Object> totalCustom = new HashMap<>(custom);
    totalCustom.putAll(extraCustom);
    Data data = new Data.Builder()
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
        .custom(totalCustom)
        .notifier(notifier)
        .timestamp(timestamp)
        .body(body)
        .level(level)
        .build();

    when(bodyFactory.from(eq(error), any())).thenReturn(body);
    when(contextProvider.provide()).thenReturn(context);
    when(personProvider.provide()).thenReturn(person);
    when(serverProvider.provide()).thenReturn(server);
    when(clientProvider.provide()).thenReturn(client);
    when(customProvider.provide()).thenReturn(custom);
    when(notifierProvider.provide()).thenReturn(notifier);
    when(timestampProvider.provide()).thenReturn(timestamp);
    when(requestProvider.provide()).thenReturn(request);
    when(transformer.transform(data)).thenReturn(data);
    when(fingerprintGenerator.from(data)).thenReturn(fingerprint);
    when(uuidGenerator.from(data)).thenReturn(uuid);

    Rollbar sut = new Rollbar(config, bodyFactory);

    sut.log(error, extraCustom, description, level);

    Data dataExpected = new Data.Builder(data)
        .fingerprint(fingerprint)
        .uuid(uuid)
        .build();

    Payload expected = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(dataExpected)
        .build();

    verify(contextProvider).provide();
    verify(requestProvider).provide();
    verify(personProvider).provide();
    verify(serverProvider).provide();
    verify(clientProvider).provide();
    verify(customProvider).provide();
    verify(notifierProvider).provide();
    verify(timestampProvider).provide();
    verify(filter).preProcess(level, error, extraCustom, description);
    verify(transformer).transform(data);
    verify(uuidGenerator).from(data);
    verify(fingerprintGenerator).from(data);
    verify(filter).postProcess(dataExpected);
    verify(sender).send(expected);
  }

  @Test
  public void shouldPrefilterWithoutGatheringData() {
    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    String description = "description";
    Map<String, Object> custom = new HashMap<>();

    when(filter.preProcess(level, error, custom, description)).thenReturn(true);

    Rollbar sut = new Rollbar(config, bodyFactory);

    sut.log(error, custom, description, level);

    verify(contextProvider, never()).provide();
    verify(requestProvider, never()).provide();
    verify(personProvider, never()).provide();
    verify(serverProvider, never()).provide();
    verify(clientProvider, never()).provide();
    verify(customProvider, never()).provide();
    verify(notifierProvider, never()).provide();
    verify(timestampProvider, never()).provide();
    verify(filter).preProcess(level, error, custom, description);
    verify(transformer, never()).transform(any());
    verify(uuidGenerator, never()).from(any());
    verify(fingerprintGenerator, never()).from(any());
    verify(filter, never()).postProcess(any());
    verify(sender, never()).send(any());
  }

  @Test
  public void shouldPostFilterWithoutSendingPayload() {
    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    String description = "description";
    Map<String, Object> custom = new HashMap<>();

    when(transformer.transform(any())).thenReturn(mock(Data.class));
    when(filter.preProcess(level, error, custom, description)).thenReturn(false);
    when(filter.postProcess(any())).thenReturn(true);

    Rollbar sut = new Rollbar(config, bodyFactory);

    sut.log(error, custom, description, level);

    verify(sender, never()).send(any());
  }

  @Test
  public void shouldUseTransformedData() {
    Config config = new ConfigBuilder()
        .accessToken(ACCESS_TOKEN)
        .transformer(transformer)
        .sender(sender)
        .build();

    Data transformedData = mock(Data.class);
    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    String description = "description";
    Map<String, Object> custom = new HashMap<>();


    when(transformer.transform(any())).thenReturn(transformedData);

    Rollbar sut = new Rollbar(config, bodyFactory);

    sut.log(error, custom, description, level);

    Payload expected = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(transformedData)
        .build();

    verify(sender).send(expected);
  }

  @Test
  public void shouldUseTheCorrectLevel() {
    Config config = new ConfigBuilder()
        .timestamp(timestampProvider)
        .language(LANGUAGE)
        .notifier(notifierProvider)
        .sender(sender)
        .build();

    String description = "description";
    Body body = mock(Body.class);
    Long timestamp = 1L;

    when(timestampProvider.provide()).thenReturn(timestamp);
    when(bodyFactory.from(any(), any())).thenReturn(body);

    Rollbar sut = new Rollbar(config, bodyFactory);

    Data.Builder dataBuilder = new Data.Builder()
        .timestamp(timestamp)
        .language(LANGUAGE)
        .body(body);

    Payload warningPayload = new Payload.Builder()
        .data(dataBuilder.level(Level.WARNING).build())
        .build();
    Payload criticalPayload = new Payload.Builder()
        .data(dataBuilder.level(Level.CRITICAL).build())
        .build();
    Payload errorPayload = new Payload.Builder()
        .data(dataBuilder.level(Level.ERROR).build())
        .build();

    sut.log(null, null, description);
    verify(sender).send(warningPayload);

    sut.log(new Error("The error."), null, description);
    verify(sender).send(criticalPayload);

    sut.log(new RuntimeException("The error"), null, description);
    verify(sender).send(errorPayload);
  }

  @Test
  public void shouldNotThrowExceptionWithEmptyConfig() {
    Config config = new ConfigBuilder().build();

    Rollbar sut = Rollbar.init(config);

    Throwable error = new RuntimeException("Something went wrong.");

    sut.log(error, null, null, Level.ERROR);
  }

  @Test
  public void shouldInitializeOnceTheInternalInstance() {
    Config config = new ConfigBuilder().build();

    Rollbar sut = Rollbar.init(config);
    Rollbar other = Rollbar.init(config);

    assertThat(sut == other, is(true));
  }

  @Test
  public void shouldLogWithLogMethod() {
    Config config = new ConfigBuilder()
        .timestamp(timestampProvider)
        .language(LANGUAGE)
        .notifier(notifierProvider)
        .sender(sender)
        .build();

    Rollbar sut = new Rollbar(config, bodyFactory);

    Data.Builder dataBuilder = new Data.Builder()
        .language(LANGUAGE);

    Body bodyOnlyError = mock(Body.class);
    Body bodyOnlyDescription = mock(Body.class);
    Body body = mock(Body.class);

    Throwable error = new RuntimeException("The error");
    String description = "description";
    Map<String, Object> custom = new HashMap<>();
    custom.put("param1", "value1");

    when(bodyFactory.from(error, null)).thenReturn(bodyOnlyError);
    when(bodyFactory.from(null, description)).thenReturn(bodyOnlyDescription);
    when(bodyFactory.from(error, description)).thenReturn(body);

    sut.log(error);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyError)
        .level(sut.level(error))
        .build()
    ).build());

    sut.log(error, Level.DEBUG);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyError)
        .level(Level.DEBUG)
        .build()
    ).build());

    sut.log(description);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyDescription)
        .level(sut.level(null))
        .build()
    ).build());

    sut.log(description, Level.DEBUG);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyDescription)
        .level(Level.DEBUG)
        .build()
    ).build());

    sut.log(error, description);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(body)
        .level(sut.level(error))
        .build()
    ).build());

    sut.log(error, description, Level.DEBUG);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(body)
        .level(Level.DEBUG)
        .build()
    ).build());

    sut.log(error, custom);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyError)
        .level(sut.level(error))
        .custom(custom)
        .build()
    ).build());

    sut.log(error, custom, Level.DEBUG);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyError)
        .level(Level.DEBUG)
        .custom(custom)
        .build()
    ).build());

    sut.log(description, custom);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyDescription)
        .custom(custom)
        .level(sut.level(null))
        .build()
    ).build());

    sut.log(description, custom, Level.DEBUG);
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyDescription)
        .custom(custom)
        .level(Level.DEBUG)
        .build()
    ).build());
  }

  @Test
  public void shouldLogWithEachLevels() {
    Config config = new ConfigBuilder()
        .timestamp(timestampProvider)
        .language(LANGUAGE)
        .notifier(notifierProvider)
        .sender(sender)
        .build();

    Rollbar sut = new Rollbar(config, bodyFactory);

    for(Level level : Level.values()) {
      verifyCallsBasedOnLevels(sut, level);
    }
  }

  private void verifyCallsBasedOnLevels(Rollbar sut, Level level) {
    Data.Builder dataBuilder = new Data.Builder()
        .language(LANGUAGE);

    Body bodyOnlyError = mock(Body.class);
    Body bodyOnlyDescription = mock(Body.class);
    Body body = mock(Body.class);

    Throwable error = new RuntimeException("The error");
    String description = "description";
    Map<String, Object> custom = new HashMap<>();
    custom.put("param1", "value1");

    when(bodyFactory.from(error, null)).thenReturn(bodyOnlyError);
    when(bodyFactory.from(null, description)).thenReturn(bodyOnlyDescription);
    when(bodyFactory.from(error, description)).thenReturn(body);

    switch (level) {
      case CRITICAL:
        sut.critical(error);
        break;
      case ERROR:
        sut.error(error);
        break;
      case WARNING:
        sut.warning(error);
        break;
      case INFO:
        sut.info(error);
        break;
      case DEBUG:
        sut.debug(error);
        break;
    }
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyError)
        .level(level)
        .build()
    ).build());

    switch (level) {
      case CRITICAL:
        sut.critical(description);
        break;
      case ERROR:
        sut.error(description);
        break;
      case WARNING:
        sut.warning(description);
      case INFO:
        sut.info(description);
        break;
      case DEBUG:
        sut.debug(description);
        break;
    }
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyDescription)
        .level(level)
        .build()
    ).build());

    switch (level) {
      case CRITICAL:
        sut.critical(error, description);
        break;
      case ERROR:
        sut.error(error, description);
        break;
      case WARNING:
        sut.warning(error, description);
        break;
      case INFO:
        sut.info(error, description);
        break;
      case DEBUG:
        sut.debug(error, description);
        break;
    }
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(body)
        .level(level)
        .build()
    ).build());

    switch (level) {
      case CRITICAL:
        sut.critical(error, custom);
        break;
      case ERROR:
        sut.error(error, custom);
        break;
      case WARNING:
        sut.warning(error, custom);
        break;
      case INFO:
        sut.info(error, custom);
        break;
      case DEBUG:
        sut.debug(error, custom);
        break;
    }
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyError)
        .level(level)
        .custom(custom)
        .build()
    ).build());

    switch (level) {
      case CRITICAL:
        sut.critical(description, custom);
        break;
      case ERROR:
        sut.error(description, custom);
        break;
      case WARNING:
        sut.warning(description, custom);
        break;
      case INFO:
        sut.info(description, custom);
        break;
      case DEBUG:
        sut.debug(description, custom);
        break;
    }
    verify(sender).send(new Payload.Builder().data(dataBuilder
        .body(bodyOnlyDescription)
        .custom(custom)
        .level(level)
        .build()
    ).build());
  }
}