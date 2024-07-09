package com.rollbar.notifier.config;

import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.fingerprint.FingerprintGenerator;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.notifier.uuid.UuidGenerator;
import java.util.List;
import java.util.Map;

/**
 * Rollbar notifier settings are common to all types of notifier (sync and async).
 */
public interface CommonConfig {
  /**
   * Get the access token.
   *
   * @return the Rollbar access token.
   */
  String accessToken();

  /**
   * Get the Rollbar endpoint.
   *
   * @return the Rollbar endpoint.
   */
  String endpoint();

  /**
   * Get the environment.
   *
   * @return the environment.
   */
  String environment();

  /**
   * Get the code version.
   *
   * @return the code version.
   */
  String codeVersion();

  /**
   * Get the platform.
   *
   * @return the platform.
   */
  String platform();

  /**
   * Get the language.
   *
   * @return the language.
   */
  String language();

  /**
   * Get the framework.
   *
   * @return the framework.
   */
  String framework();

  /**
   * Get the context provider.
   *
   * @return the context.
   */
  Provider<String> context();

  /**
   * Get the {@link Request request} {@link Provider provider}.
   *
   * @return the request.
   */
  Provider<Request> request();

  /**
   * Get the {@link Person person} {@link Provider provider}.
   *
   * @return the person.
   */
  Provider<Person> person();

  /**
   * Get the {@link Server server} {@link Provider provider}.
   *
   * @return the server.
   */
  Provider<Server> server();

  /**
   * Get the {@link Client client} {@link Provider provider}.
   *
   * @return the server.
   */
  Provider<Client> client();

  /**
   * Get the custom {@link Provider provider}.
   *
   * @return the provider of any custom values.
   */
  Provider<Map<String, Object>> custom();

  /**
   * Get the {@link Notifier notifier} {@link Provider provider}.
   *
   * @return the provider of the notifier data.
   */
  Provider<Notifier> notifier();

  /**
   * Get the {@link Long timestamp} {@link Provider provider}.
   *
   * @return the provider of a timestamp.
   */
  Provider<Long> timestamp();

  /**
   * Get the {@link Filter filter}.
   *
   * @return the filter.
   */
  Filter filter();

  /**
   * Get the {@link Transformer transformer}.
   *
   * @return the transformer.
   */
  Transformer transformer();

  /**
   * Get the {@link FingerprintGenerator fingerprint generator}.
   *
   * @return the fingerprint.
   */
  FingerprintGenerator fingerPrintGenerator();

  /**
   * Get the {@link UuidGenerator UUID generator}.
   *
   * @return the uuid generator.
   */
  UuidGenerator uuidGenerator();

  /**
   * The serializer to convert a payload to JSON.
   *
   * @return The {@link JsonSerializer instance}.
   */
  JsonSerializer jsonSerializer();

  /**
   * Get the list of packages considered to be in your app.
   *
   * @return the list of packages.
   */
  List<String> appPackages();

  /**
   * Flag to indicate that the Rollbar notifier should handle the uncaught errors.
   *
   * @return true to handle otherwise false.
   */
  boolean handleUncaughtErrors();

  /**
   * Flag to indicate that the Rollbar notifier should be enabled/disabled.
   *
   * @return true if enabled otherwise false.
   */
  boolean isEnabled();

  /**
   * Level to use as the default for messages if one is not otherwise specified.
   *
   * @return the level.
   */
  Level defaultMessageLevel();

  /**
   * Level to use as the default for Errors if one is not otherwise specified.
   *
   * @return the level.
   */
  Level defaultErrorLevel();

  /**
   * Level to use as the default for non-Error Throwables if one is not otherwise specified.
   *
   * @return the level.
   */
  Level defaultThrowableLevel();


  /**
   * <p>
   * If set to true, the notifier will attempt to truncate payloads that are larger than the
   * maximum size Rollbar allows. Default: false.
   * </p>
   * @return true to truncate payloads otherwise false.
   */
  boolean truncateLargePayloads();

  int maximumTelemetryData();
}
