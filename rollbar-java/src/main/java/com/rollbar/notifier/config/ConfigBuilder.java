package com.rollbar.notifier.config;

import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.fingerprint.FingerprintGenerator;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.provider.notifier.NotifierProvider;
import com.rollbar.notifier.provider.timestamp.TimestampProvider;
import com.rollbar.notifier.sender.BufferedSender;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.notifier.uuid.UuidGenerator;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;

/**
 * Configuration builder to build the {@link Config configuration} of the {@link Rollbar rollbar}
 * notifier.
 */
public class ConfigBuilder {

  private String accessToken;

  private String environment;

  private String codeVersion;

  private String platform;

  private String language;

  private String framework;

  private Provider<String> context;

  private Provider<Request> request;

  private Provider<Person> person;

  private Provider<Server> server;

  private Provider<Client> client;

  private Provider<Map<String, Object>> custom;

  private Provider<Notifier> notifier;

  private Provider<Long> timestamp;

  private Filter filter;

  private Transformer transformer;

  private FingerprintGenerator fingerPrintGenerator;

  private UuidGenerator uuidGenerator;

  private Sender sender;

  private boolean handleUncaughtErrors;

  private boolean enabled;

  /**
   * Constructor with an access token.
   */
  private ConfigBuilder(String accessToken) {
    // Defaults
    this.accessToken = accessToken;
    this.language = "java";
    this.handleUncaughtErrors = true;
    this.enabled = true;
  }

  /**
   * Constructor from another Config.
   */
  private ConfigBuilder(Config config) {
    this.accessToken = config.accessToken();
    this.environment = config.environment();
    this.codeVersion = config.codeVersion();
    this.platform = config.platform();
    this.language = config.language();
    this.framework = config.framework();
    this.context = config.context();
    this.request = config.request();
    this.person = config.person();
    this.server = config.server();
    this.client = config.client();
    this.custom = config.custom();
    this.notifier = config.notifier();
    this.timestamp = config.timestamp();
    this.filter = config.filter();
    this.transformer = config.transformer();
    this.fingerPrintGenerator = config.fingerPrintGenerator();
    this.uuidGenerator = config.uuidGenerator();
    this.sender = config.sender();
    this.handleUncaughtErrors = config.handleUncaughtErrors();
    this.enabled = config.isEnabled();
  }

  /**
   * Initializes a config builder instance with the access token supplied.
   * @param accessToken the access token.
   * @return the builder instance.
   */
  public static ConfigBuilder withAccessToken(String accessToken) {
    return new ConfigBuilder(accessToken);
  }

  /**
   * Initializes a config builder instance with the values set in the supplied config.
   * @param config an object conforming to the {@link Config} interface.
   * @return the builder instance.
   */
  public static ConfigBuilder withConfig(Config config) {
    return new ConfigBuilder(config);
  }

  /**
   * The access token to use.
   *
   * @param accessToken the access token.
   * @return the builder instance.
   */
  public ConfigBuilder accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  /**
   * Represents the current environment (e.g.: production, debug, test).
   *
   * @param environment the environment.
   * @return the builder instance.
   */
  public ConfigBuilder environment(String environment) {
    this.environment = environment;
    return this;
  }

  /**
   * The currently running version of the code.
   *
   * @param codeVersion the code version.
   * @return the builder instance.
   */
  public ConfigBuilder codeVersion(String codeVersion) {
    this.codeVersion = codeVersion;
    return this;
  }

  /**
   * The platform running (most likely JVM and a version).
   *
   * @param platform the platform.
   * @return the builder instance.
   */
  public ConfigBuilder platform(String platform) {
    this.platform = platform;
    return this;
  }

  /**
   * The language running (most likely java, but any JVM language might be here).
   *
   * @param language the language.
   * @return the builder instance.
   */
  public ConfigBuilder language(String language) {
    this.language = language;
    return this;
  }

  /**
   * The framework being run (e.g. Play, Spring, etc).
   *
   * @param framework the framework.
   * @return the builder instance.
   */
  public ConfigBuilder framework(String framework) {
    this.framework = framework;
    return this;
  }

  /**
   * The provider to retrieve the context.
   *
   * @param context the context provider.
   * @return the builder instance.
   */
  public ConfigBuilder context(Provider<String> context) {
    this.context = context;
    return this;
  }

  /**
   * The provider to retrieve the {@link Request request}.
   *
   * @param request the request provider.
   * @return the builder instance.
   */
  public ConfigBuilder request(Provider<Request> request) {
    this.request = request;
    return this;
  }

  /**
   * The provider to retrieve the {@link Person person}.
   *
   * @param person the person provider.
   * @return the builder instance.
   */
  public ConfigBuilder person(Provider<Person> person) {
    this.person = person;
    return this;
  }

  /**
   * The provider to retrieve the {@link Server server}.
   *
   * @param server the server provider.
   * @return the builder instance.
   */
  public ConfigBuilder server(Provider<Server> server) {
    this.server = server;
    return this;
  }

  /**
   * The provider to retrieve the {@link Client client}.
   *
   * @param client the client provider.
   * @return the builder instance.
   */
  public ConfigBuilder client(Provider<Client> client) {
    this.client = client;
    return this;
  }

  /**
   * The provider to retrieve the custom.
   *
   * @param custom the custom provider.
   * @return the builder instance.
   */
  public ConfigBuilder custom(Provider<Map<String, Object>> custom) {
    this.custom = custom;
    return this;
  }

  /**
   * The provider to retrieve the {@link Notifier notifier}.
   *
   * @param notifier the notifier provider.
   * @return the builder instance.
   */
  public ConfigBuilder notifier(Provider<Notifier> notifier) {
    this.notifier = notifier;
    return this;
  }

  /**
   * The provider to retrieve the timestamp.
   * @param timestamp the timestamp.
   * @return the builder instance.
   */
  public ConfigBuilder timestamp(Provider<Long> timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * The provider to retrieve the {@link Filter filter}.
   *
   * @param filter the filter provider.
   * @return the builder instance.
   */
  public ConfigBuilder filter(Filter filter) {
    this.filter = filter;
    return this;
  }

  /**
   * The provider to retrieve the {@link Transformer transformer}.
   *
   * @param transformer the transformer provider.
   * @return the builder instance.
   */
  public ConfigBuilder transformer(Transformer transformer) {
    this.transformer = transformer;
    return this;
  }

  /**
   * The provider to retrieve the {@link FingerprintGenerator fingerprint generator}.
   *
   * @param fingerPrintGenerator the fingerprint generator provider.
   * @return the builder instance.
   */
  public ConfigBuilder fingerPrintGenerator(FingerprintGenerator fingerPrintGenerator) {
    this.fingerPrintGenerator = fingerPrintGenerator;
    return this;
  }

  /**
   * The provider to retrieve the {@link UuidGenerator uuid generator}.
   *
   * @param uuidGenerator the uuid generator provider.
   * @return the builder instance.
   */
  public ConfigBuilder uuidGenerator(UuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
    return this;
  }

  /**
   * Retrieve the {@link Sender sender}.
   *
   * @param sender the sender.
   * @return the builder instance.
   */
  public ConfigBuilder sender(Sender sender) {
    this.sender = sender;
    return this;
  }

  /**
   * Flag to set the default handler for uncaught errors,
   * see {@link UncaughtExceptionHandler}.
   * @param handleUncaughtErrors true to handle uncaught errors otherwise false.
   * @return the builder instance.
   */
  public ConfigBuilder handleUncaughtErrors(boolean handleUncaughtErrors) {
    this.handleUncaughtErrors = handleUncaughtErrors;
    return this;
  }

  /**
   * Flag to indicate that the Rollbar notifier should enabled/disabled.
   *
   * @param enabled true to enable the notifier.
   * @return the builder instance.
   */
  public ConfigBuilder enabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Builds the {@link Config config}.
   *
   * @return the config.
   */
  public Config build() {
    if (this.notifier == null) {
      this.notifier = new NotifierProvider();
    }
    if (this.sender == null) {
      this.sender = new BufferedSender.Builder()
          .sender(
            new SyncSender.Builder()
            .accessToken(accessToken)
            .build()
      ).build();
    }
    if (this.timestamp == null) {
      this.timestamp = new TimestampProvider();
    }

    return new ConfigImpl(this);
  }

  private static class ConfigImpl implements Config {

    private final String accessToken;

    private final String environment;

    private final String codeVersion;

    private final String platform;

    private final String language;

    private final String framework;

    private final Provider<String> context;

    private final Provider<Request> request;

    private final Provider<Person> person;

    private final Provider<Server> server;

    private final Provider<Client> client;

    private final Provider<Map<String, Object>> custom;

    private final Provider<Notifier> notifier;

    private final Provider<Long> timestamp;

    private final Filter filter;

    private final Transformer transformer;

    private final FingerprintGenerator fingerPrintGenerator;

    private final UuidGenerator uuidGenerator;

    private final Sender sender;

    private final boolean handleUncaughtErrors;

    private final boolean enabled;

    ConfigImpl(ConfigBuilder builder) {
      this.accessToken = builder.accessToken;
      this.environment = builder.environment;
      this.codeVersion = builder.codeVersion;
      this.platform = builder.platform;
      this.language = builder.language;
      this.framework = builder.framework;
      this.context = builder.context;
      this.request = builder.request;
      this.person = builder.person;
      this.server = builder.server;
      this.client = builder.client;
      this.custom = builder.custom;
      this.notifier = builder.notifier;
      this.timestamp = builder.timestamp;
      this.filter = builder.filter;
      this.transformer = builder.transformer;
      this.fingerPrintGenerator = builder.fingerPrintGenerator;
      this.uuidGenerator = builder.uuidGenerator;
      this.sender = builder.sender;
      this.handleUncaughtErrors = builder.handleUncaughtErrors;
      this.enabled = builder.enabled;
    }

    @Override
    public String accessToken() {
      return accessToken;
    }

    @Override
    public String environment() {
      return environment;
    }

    @Override
    public String codeVersion() {
      return codeVersion;
    }

    @Override
    public String platform() {
      return platform;
    }

    @Override
    public String language() {
      return language;
    }

    @Override
    public String framework() {
      return framework;
    }

    @Override
    public Provider<String> context() {
      return context;
    }

    @Override
    public Provider<Request> request() {
      return request;
    }

    @Override
    public Provider<Person> person() {
      return person;
    }

    @Override
    public Provider<Server> server() {
      return server;
    }

    @Override
    public Provider<Client> client() {
      return client;
    }

    @Override
    public Provider<Map<String, Object>> custom() {
      return custom;
    }

    @Override
    public Provider<Notifier> notifier() {
      return notifier;
    }

    @Override
    public Provider<Long> timestamp() {
      return timestamp;
    }

    @Override
    public Filter filter() {
      return filter;
    }

    @Override
    public Transformer transformer() {
      return transformer;
    }

    @Override
    public FingerprintGenerator fingerPrintGenerator() {
      return fingerPrintGenerator;
    }

    @Override
    public UuidGenerator uuidGenerator() {
      return uuidGenerator;
    }

    @Override
    public Sender sender() {
      return sender;
    }

    @Override
    public boolean handleUncaughtErrors() {
      return handleUncaughtErrors;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }
  }
}
