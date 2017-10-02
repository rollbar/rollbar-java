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
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.notifier.uuid.UuidGenerator;
import java.util.Map;

/**
 * The configuration for the {@link Rollbar notifier}.
 */
public interface Config {

  /**
   * Get the access token.
   *
   * @return the Rollbar access token.
   */
  String accessToken();

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
   */
  Provider<Map<String, Object>> custom();

  /**
   * Get the {@link Notifier notifier} {@link Provider provider}.
   */
  Provider<Notifier> notifier();

  /**
   * Get the {@link Long timestamp} {@link Provider provider}.
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
   * Get the {@link Sender sender}.
   *
   * @return the sender.
   */
  Sender sender();

  /**
   * Flag to indicate that the Rollbar notifier should handle the uncaught errors.
   *
   * @return true to handle otherwise false.
   */
  boolean handleUncaughtErrors();
}
