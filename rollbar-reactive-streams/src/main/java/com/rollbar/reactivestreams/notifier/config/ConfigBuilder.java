package com.rollbar.reactivestreams.notifier.config;

import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.DefaultLevels;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.fingerprint.FingerprintGenerator;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.provider.notifier.NotifierProvider;
import com.rollbar.notifier.provider.timestamp.TimestampProvider;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.notifier.uuid.UuidGenerator;
import com.rollbar.reactivestreams.notifier.sender.AsyncSender;
import com.rollbar.reactivestreams.notifier.sender.Sender;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpClient;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpClientFactory;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration builder to build the {@link Config configuration} of the {@link Rollbar rollbar}
 * notifier.
 */
public final class ConfigBuilder {

  private String accessToken;
  private String endpoint;
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
  private AsyncHttpClient httpClient;
  private Sender asyncSender;
  private JsonSerializer jsonSerializer;
  private List<String> appPackages;
  private boolean handleUncaughtErrors;
  private boolean enabled;
  private DefaultLevels defaultLevels;
  private boolean truncateLargePayloads;

  /**
   * Constructor with an access token.
   */
  protected ConfigBuilder(String accessToken) {
    // Defaults
    this.accessToken = accessToken;
    this.handleUncaughtErrors = true;
    this.enabled = true;
    this.defaultLevels = new DefaultLevels();
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
    this.asyncSender = config.asyncSender();
    this.handleUncaughtErrors = config.handleUncaughtErrors();
    this.enabled = config.isEnabled();
    this.endpoint = config.endpoint();
    this.appPackages = config.appPackages();
    this.defaultLevels = new DefaultLevels(config);
    this.truncateLargePayloads = config.truncateLargePayloads();
  }

  private ConfigBuilder(Sender sender) {
    this((String)null);
    this.asyncSender = sender;
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
   * Initializes a config builder instance with the sender supplied.
   * @param sender the AsyncSender
   * @return the builder instance.
   */
  public static ConfigBuilder withSender(Sender sender) {
    return new ConfigBuilder(sender);
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
   * The Rollbar endpoint to use.
   *
   * @param endpoint the Rollbar endpoint url.
   * @return the builder instance.
   */
  public ConfigBuilder endpoint(String endpoint) {
    this.endpoint = endpoint;
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
   *
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
   * The {@link AsyncHttpClient HTTP client}.
   *
   * @param httpClient the HTTP client.
   * @return the builder instance.
   */
  public ConfigBuilder httpClient(AsyncHttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  /**
   * The {@link Sender sender}.
   * <p>
   * Note: Setting the sender overrides the {@link #httpClient(AsyncHttpClient)} and
   * {@link #accessToken(String)} settings.
   * </p>
   * @param sender the sender.
   * @return the builder instance.
   */
  public ConfigBuilder sender(Sender sender) {
    this.asyncSender = sender;
    return this;
  }

  /**
   * The JsonSerializer to use with the default Sender if no other
   * sender is specified. If a sender is specified then this
   * parameter is ignored.
   *
   * @param jsonSerializer the json serializer.
   * @return the builder instance.
   */
  public ConfigBuilder jsonSerializer(JsonSerializer jsonSerializer) {
    this.jsonSerializer = jsonSerializer;
    return this;
  }

  /**
   * The list of packages to be considered in your app.
   *
   * @param appPackages the list of packages.
   * @return the builder instance.
   */
  public ConfigBuilder appPackages(List<String> appPackages) {
    this.appPackages = appPackages;
    return this;
  }

  /**
   * Flag to set the default handler for uncaught errors,
   * see {@link UncaughtExceptionHandler}.
   *
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
   * Level to use as the default for messages if one is not otherwise specified.
   *
   * @param level the level.
   * @return the builder instance.
   */
  public ConfigBuilder defaultMessageLevel(Level level) {
    this.defaultLevels.setMessage(level);
    return this;
  }

  /**
   * Level to use as the default for Errors if one is not otherwise specified.
   *
   * @param level the level.
   * @return the builder instance.
   */
  public ConfigBuilder defaultErrorLevel(Level level) {
    this.defaultLevels.setError(level);
    return this;
  }

  /**
   * Level to use as the default for non-Error Throwables if one is not otherwise specified.
   *
   * @param level the level.
   * @return the builder instance.
   */
  public ConfigBuilder defaultThrowableLevel(Level level) {
    this.defaultLevels.setThrowable(level);
    return this;
  }

  /**
   * <p>
   * If set to true, the notifier will attempt to truncate payloads that are larger than the
   * maximum size Rollbar allows. Default: false.
   * </p>
   * @param truncate true to enable truncation.
   * @return the builder instance.
   */
  public ConfigBuilder truncateLargePayloads(boolean truncate) {
    this.truncateLargePayloads = truncate;
    return this;
  }

  /**
   * Builds the {@link Config config}.
   *
   * @return the config.
   */
  public Config build() {
    if (this.language == null) {
      this.language = "java";
    }
    if (this.endpoint == null) {
      this.endpoint = SyncSender.DEFAULT_API_ENDPOINT;
    }
    if (this.notifier == null) {
      this.notifier = new NotifierProvider();
    }
    if (this.asyncSender == null) {
      AsyncHttpClient httpClient = this.httpClient;
      if (httpClient == null) {
        httpClient = AsyncHttpClientFactory.defaultClient();
      }
      AsyncSender.Builder senderBuilder = new AsyncSender.Builder(httpClient, this.endpoint)
              .accessToken(accessToken);

      if (this.jsonSerializer != null) {
        senderBuilder.jsonSerializer(this.jsonSerializer);
      }

      this.asyncSender = senderBuilder.build();
    }
    if (this.timestamp == null) {
      this.timestamp = new TimestampProvider();
    }

    return new ConfigImpl(this);
  }

  private static class ConfigImpl implements Config {

    private final String accessToken;
    private final String endpoint;
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
    private final Sender asyncSender;
    private final List<String> appPackages;
    private final boolean handleUncaughtErrors;
    private final boolean enabled;
    private final DefaultLevels defaultLevels;
    private final JsonSerializer jsonSerializer;
    private final boolean truncateLargePayloads;

    ConfigImpl(ConfigBuilder builder) {
      this.accessToken = builder.accessToken;
      this.endpoint = builder.endpoint;
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
      this.asyncSender = builder.asyncSender;
      if (builder.appPackages == null) {
        this.appPackages = Collections.emptyList();
      } else {
        this.appPackages = builder.appPackages;
      }
      this.handleUncaughtErrors = builder.handleUncaughtErrors;
      this.enabled = builder.enabled;
      this.defaultLevels = builder.defaultLevels;
      this.jsonSerializer = builder.jsonSerializer;
      this.truncateLargePayloads = builder.truncateLargePayloads;
    }

    @Override
    public String accessToken() {
      return accessToken;
    }

    @Override
    public String endpoint() {
      return endpoint;
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
    public JsonSerializer jsonSerializer() {
      return jsonSerializer;
    }

    @Override
    public Sender asyncSender() {
      return asyncSender;
    }

    @Override
    public List<String> appPackages() {
      return appPackages;
    }

    @Override
    public boolean handleUncaughtErrors() {
      return handleUncaughtErrors;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public Level defaultMessageLevel() {
      return defaultLevels.getMessage();
    }

    @Override
    public Level defaultErrorLevel() {
      return defaultLevels.getError();
    }

    @Override
    public Level defaultThrowableLevel() {
      return defaultLevels.getThrowable();
    }

    @Override
    public boolean truncateLargePayloads() {
      return truncateLargePayloads;
    }
  }
}
