package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.data.body.Body;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the actual data being posted to Rollbar.
 */
public class Data implements JsonSerializable {

  private final String environment;

  private final Body body;

  private final Level level;

  private final Long timestamp;

  private final String codeVersion;

  private final String platform;

  private final String language;

  private final String framework;

  private final String context;

  private final Request request;

  private final Person person;

  private final Server server;

  private final Client client;

  private final Map<String, Object> custom;

  private final String fingerprint;

  private final String title;

  private final String uuid;

  private final boolean isUncaught;

  private final Notifier notifier;

  private Data(Builder builder) {
    this.environment = builder.environment;
    this.body = builder.body;
    this.level = builder.level;
    this.timestamp = builder.timestamp;
    this.codeVersion = builder.codeVersion;
    this.platform = builder.platform;
    this.language = builder.language;
    this.framework = builder.framework;
    this.context = builder.context;
    this.request = builder.request;
    this.person = builder.person;
    this.server = builder.server;
    this.client = builder.client;
    this.custom = builder.custom != null ? builder.custom : null;
    this.fingerprint = builder.fingerprint;
    this.title = builder.title;
    this.uuid = builder.uuid;
    this.isUncaught = builder.isUncaught;
    this.notifier = builder.notifier;
  }

  /**
   * Getter.
   *
   * @return the environment.
   */
  public String getEnvironment() {
    return this.environment;
  }

  /**
   * Getter.
   *
   * @return the body.
   */
  public Body getBody() {
    return this.body;
  }

  /**
   * Getter.
   *
   * @return the level
   */
  public Level getLevel() {
    return this.level;
  }

  /**
   * Getter.
   *
   * @return the timestamp.
   */
  public Long getTimestamp() {
    return this.timestamp;
  }

  /**
   * Getter.
   *
   * @return the code version.
   */
  public String getCodeVersion() {
    return this.codeVersion;
  }

  /**
   * Getter.
   *
   * @return the platform.
   */
  public String getPlatform() {
    return this.platform;
  }

  /**
   * Getter.
   *
   * @return the language.
   */
  public String getLanguage() {
    return this.language;
  }

  /**
   * Getter.
   *
   * @return the framework.
   */
  public String getFramework() {
    return this.framework;
  }

  /**
   * Getter.
   *
   * @return the context.
   */
  public String getContext() {
    return this.context;
  }

  /**
   * Getter.
   *
   * @return the request.
   */
  public Request getRequest() {
    return this.request;
  }

  /**
   * Getter.
   *
   * @return the person.
   */
  public Person getPerson() {
    return this.person;
  }

  /**
   * Getter.
   *
   * @return the client.
   */
  public Client getClient() {
    return this.client;
  }

  /**
   * Getter.
   *
   * @return the server.
   */
  public Server getServer() {
    return this.server;
  }

  /**
   * Getter.
   *
   * @return the custom data.
   */
  public Map<String, Object> getCustom() {
    return custom;
  }

  /**
   * Getter.
   *
   * @return the fingerprint.
   */
  public String getFingerprint() {
    return this.fingerprint;
  }

  /**
   * Getter.
   *
   * @return the title.
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * Getter.
   *
   * @return the uuid.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Getter.
   *
   * @return whether or not this data comes from an uncaught exception
   */
  public boolean isUncaught() {
    return isUncaught;
  }

  /**
   * Getter.
   *
   * @return the notifier.
   */
  public Notifier getNotifier() {
    return this.notifier;
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();

    if (environment != null) {
      values.put("environment", environment);
    }
    if (body != null) {
      values.put("body", body);
    }
    if (level != null) {
      values.put("level", level.asJson());
    }
    if (timestamp != null) {
      double timestamp_secs = timestamp / 1000.0;
      values.put("timestamp", timestamp_secs);
    }
    if (codeVersion != null) {
      values.put("code_version", codeVersion);
    }
    if (platform != null) {
      values.put("platform", platform);
    }
    if (language != null) {
      values.put("language", language);
    }
    if (framework != null) {
      values.put("framework", framework);
    }
    if (context != null) {
      values.put("context", context);
    }
    if (request != null) {
      values.put("request", request);
    }
    if (person != null) {
      values.put("person", person);
    }
    if (server != null) {
      values.put("server", server);
    }
    if (client != null) {
      values.put("client", client);
    }
    if (custom != null) {
      values.put("custom", custom);
    }
    if (fingerprint != null) {
      values.put("fingerprint", fingerprint);
    }
    if (title != null) {
      values.put("title", title);
    }
    if (uuid != null) {
      values.put("uuid", uuid);
    }
    if (notifier != null) {
      values.put("notifier", notifier);
    }

    return values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Data data = (Data) o;
    return Objects.equals(environment, data.environment)
        && Objects.equals(body, data.body)
        && level == data.level
        && Objects.equals(timestamp, data.timestamp)
        && Objects.equals(codeVersion, data.codeVersion)
        && Objects.equals(platform, data.platform)
        && Objects.equals(language, data.language)
        && Objects.equals(framework, data.framework)
        && Objects.equals(context, data.context)
        && Objects.equals(request, data.request)
        && Objects.equals(person, data.person)
        && Objects.equals(server, data.server)
        && Objects.equals(client, data.client)
        && Objects.equals(custom, data.custom)
        && Objects.equals(fingerprint, data.fingerprint)
        && Objects.equals(title, data.title)
        && Objects.equals(uuid, data.uuid)
        && isUncaught == data.isUncaught
        && Objects.equals(notifier, data.notifier);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(environment, body, level, timestamp, codeVersion, platform, language, framework,
            context, request, person, server, client, custom, fingerprint, title, uuid, isUncaught, notifier);
  }

  @Override
  public String toString() {
    return "Data{"
        + "environment='" + environment + '\''
        + ", body=" + body
        + ", level=" + level
        + ", timestamp=" + timestamp
        + ", codeVersion='" + codeVersion + '\''
        + ", platform='" + platform + '\''
        + ", language='" + language + '\''
        + ", framework='" + framework + '\''
        + ", context='" + context + '\''
        + ", request=" + request
        + ", person=" + person
        + ", server=" + server
        + ", client=" + client
        + ", custom=" + custom
        + ", fingerprint='" + fingerprint + '\''
        + ", title='" + title + '\''
        + ", uuid='" + uuid + '\''
        + ", isUncaught='" + isUncaught + '\''
        + ", notifier=" + notifier
        + '}';
  }

  /**
   * Builder class for {@link Data data}.
   */
  public static final class Builder {

    private String environment;

    private Body body;

    private Level level;

    private Long timestamp;

    private String codeVersion;

    private String platform;

    private String language;

    private String framework;

    private String context;

    private Request request;

    private Person person;

    private Server server;

    private Client client;

    private Map<String, Object> custom;

    private String fingerprint;

    private String title;

    private String uuid;

    private boolean isUncaught;

    private Notifier notifier;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param data the {@link Data data} to initialize a new builder instance.
     */
    public Builder(Data data) {
      this.environment = data.environment;
      this.body = data.body;
      this.level = data.level;
      this.timestamp = data.timestamp;
      this.codeVersion = data.codeVersion;
      this.platform = data.platform;
      this.language = data.language;
      this.framework = data.framework;
      this.context = data.context;
      this.request = data.request;
      this.person = data.person;
      this.server = data.server;
      this.client = data.client;
      this.custom = data.custom;
      this.fingerprint = data.fingerprint;
      this.title = data.title;
      this.uuid = data.uuid;
      this.isUncaught = data.isUncaught;
      this.notifier = data.notifier;
    }

    /**
     * Represents the current environment (e.g.: production, debug, test).
     *
     * @param environment the environment.
     * @return the builder instance.
     */
    public Builder environment(String environment) {
      this.environment = environment;
      return this;
    }

    /**
     * The actual {@link Body data} being sent to rollbar (not metadata, about the request, server,
     * etc.).
     *
     * @param body the body.
     * @return the builder instance.
     */
    public Builder body(Body body) {
      this.body = body;
      return this;
    }

    /**
     * The rollbar error level.
     *
     * @param level the level.
     * @return the builder instance.
     */
    public Builder level(String level) {
      this.level = Level.valueOf(level);
      return this;
    }

    /**
     * The rollbar error {@link Level level}.
     *
     * @param level the level.
     * @return the builder instance.
     */
    public Builder level(Level level) {
      this.level = level;
      return this;
    }

    /**
     * The moment the bug happened, visible in ui as client_timestamp.
     *
     * @param timestamp the timestamp as a Long with millisecond precision.
     *        This vaulue is converted to seconds when serialized to JSON
     *        to conform to the API spec.
     * @return the builder instance.
     */
    public Builder timestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    /**
     * The currently running version of the code.
     *
     * @param codeVersion the code version.
     * @return the builder instance.
     */
    public Builder codeVersion(String codeVersion) {
      this.codeVersion = codeVersion;
      return this;
    }

    /**
     * The platform running (most likely JVM and a version).
     *
     * @param platform the platform.
     * @return the builder instance.
     */
    public Builder platform(String platform) {
      this.platform = platform;
      return this;
    }

    /**
     * The language running (most likely java, but any JVM language might be here).
     *
     * @param language the language.
     * @return the builder instance.
     */
    public Builder language(String language) {
      this.language = language;
      return this;
    }

    /**
     * The framework being run (e.g. Play, Spring, etc).
     *
     * @param framework the framework.
     * @return the builder instance.
     */
    public Builder framework(String framework) {
      this.framework = framework;
      return this;
    }

    /**
     * Custom identifier to help find where the error came from, Controller class name, for
     * instance.
     *
     * @param context the context.
     * @return the builder instance.
     */
    public Builder context(String context) {
      this.context = context;
      return this;
    }

    /**
     * Data about the Http Request that caused this, if applicable.
     *
     * @param request the request.
     * @return the builder instance.
     */
    public Builder request(Request request) {
      this.request = request;
      return this;
    }

    /**
     * Data about the user that experienced the error, if possible.
     *
     * @param person the person.
     * @return the builder instance.
     */
    public Builder person(Person person) {
      this.person = person;
      return this;
    }

    /**
     * Data about the machine on which the error occurred.
     *
     * @param server the server.
     * @return the builder instance.
     */
    public Builder server(Server server) {
      this.server = server;
      return this;
    }

    /**
     * Data about the client device this event occurred on. As there can be multiple client
     * environments for a given event (i.e. Flash running inside an HTML page), data should be
     * namespaced by platform.
     *
     * @param client the client.
     * @return the builder instance.
     */
    public Builder client(Client client) {
      this.client = client;
      return this;
    }

    /**
     * Custom data that will aid in debugging the error.
     *
     * @param custom the custom.
     * @return the builder instance.
     */
    public Builder custom(Map<String, Object> custom) {
      this.custom = custom;
      return this;
    }

    /**
     * Override the default and custom grouping with a string, if over 255 characters will be
     * hashed.
     *
     * @param fingerprint the fingerprint.
     * @return the builder instance.
     */
    public Builder fingerprint(String fingerprint) {
      this.fingerprint = fingerprint;
      return this;
    }

    /**
     * The title, max length 255 characters, overrides the default and custom ones set by Rollbar.
     *
     * @param title the title.
     * @return the builder instance.
     */
    public Builder title(String title) {
      this.title = title;
      return this;
    }

    /**
     * Override the error UUID, unique to each project, used to deduplicate occurrences.
     *
     * @param uuid the uuid.
     * @return the builder instance.
     */
    public Builder uuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * Specify whether this data object originates from an uncaught exception or not.
     *
     * @param isUncaught true if this comes from an uncaught exception.
     * @return the builder instance.
     */
    public Builder isUncaught(boolean isUncaught) {
      this.isUncaught = isUncaught;
      return this;
    }

    /**
     * Information about this notifier, esp. if creating a framework specific notifier
     *
     * @param notifier the notifier.
     * @return the builder instance.
     */
    public Builder notifier(Notifier notifier) {
      this.notifier = notifier;
      return this;
    }

    /**
     * Builds the {@link Data data}.
     *
     * @return the data.
     */
    public Data build() {
      return new Data(this);
    }
  }
}
