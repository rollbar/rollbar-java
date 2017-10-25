package com.rollbar;

import com.rollbar.api.payload.data.*;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.BufferedSender;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.transformer.Transformer;
import java.util.Map;


/**
 * This class is deprecated and provided as a convenience to ease the migration path
 * from 0.5.4 to 1.0.0. For the simplest use cases, this class should provide the same
 * functionality as the old com.rollbar.Rollbar class by delegating to the new
 * com.rollbar.notifier.Rollbar class. For any new usage, do not use this class, prefer
 * com.rollbar.notifier.Rollbar.
 */
@Deprecated
public class Rollbar {
  private final com.rollbar.notifier.Rollbar rollbar;

  /**
   * Construct a notifier defaults for everything including Sender.
   * Caution: default sender is slow and blocking. Consider providing a Sender overload.
   * @param accessToken not nullable, the access token to send payloads to
   * @param environment not nullable, the environment to send payloads under
   */
  public Rollbar(String accessToken, String environment) {
    this(accessToken, environment, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  /**
   * Construct notifier, defaults for everything but Sender.
   * @param accessToken not nullable, the access token to send payloads to
   * @param environment not nullable, the environment to send payloads under
   * @param sender the sender to use. If null uses default: {@link Sender}
   */
  public Rollbar(String accessToken, String environment, Sender sender) {
    this(accessToken, environment, sender, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  /**
   * Construct notifier with static values for all configuration options set. Anything left null will use the default
   * value. If appropriate.
   * @param accessToken not nullable, the access token to send payloads to
   * @param environment not nullable, the environment to send payloads under
   * @param sender the sender to use. If null uses default: {@link Sender}
   * @param codeVersion the version of the code currently running. If code checked out on server: `git rev-parse HEAD`
   * @param platform the platform you're running. (JVM version, or similar).
   * @param language the main language you're running ("java" by default, override w/ "clojure", "scala" etc.).
   * @param framework the framework you're using ("Play", "Spring", etc.).
   * @param context a mnemonic for finding the code responsible (e.g. controller name, module name)
   * @param request the HTTP request that triggered this error. Can be set if the IOC container can work per-request.
   * @param person the affected person. Can be set if the IOC container can work per-request.
   * @param server info about this server. This can be statically set.
   * @param custom custom info to send with *every* error. Can be dynamically or statically set.
   * @param notifier information about this notifier. Default {@code new Notifier()} ({@link Notifier}.
   * @param responseHandler what to do with the response. Use this to check for failures and handle some other way.
   * @param filter filter used to determine if you will send payload. Receives *transformed* payload.
   * @param transform alter payload before sending.
   */
  public Rollbar(String accessToken, String environment, Sender sender, String codeVersion, String platform,
                 String language, String framework, final String context, final Request request, final Person person, final Server server,
                 final Map<String, Object> custom, final Notifier notifier, SenderListener responseHandler,
                 Filter filter, Transformer transform) {
    sender = sender != null ? sender : new BufferedSender.Builder().build();
    if (responseHandler != null) {
      sender.addListener(responseHandler);
    }
    this.rollbar = new com.rollbar.notifier.Rollbar(ConfigBuilder.withAccessToken(accessToken)
        .environment(environment)
        .sender(sender)
        .codeVersion(codeVersion)
        .platform(platform)
        .language(language)
        .framework(framework)
        .context(context != null ? new Provider<String>() {
          @Override
          public String provide() {
            return context;
          }
        } : null)
        .request(request != null ? new Provider<Request>() {
          @Override
          public Request provide() {
            return request;
          }
        } : null)
        .person(person != null ? new Provider<Person>() {
          @Override
          public Person provide() {
            return person;
          }
        } : null)
        .server(server != null ? new Provider<Server>() {
          @Override
          public Server provide() {
            return server;
          }
        } : null)
        .custom(custom != null ? new Provider<Map<String, Object>>() {
          @Override
          public Map<String, Object> provide() {
            return custom;
          }
        } : null)
        .notifier(notifier != null ? new Provider<Notifier>() {
          @Override
          public Notifier provide() {
            return notifier;
          }
        } : null)
        .filter(filter)
        .transformer(transform)
        .build());
  }

  /**
   * Handle all uncaught errors on current thread with this `Rollbar`
   */
  public void handleUncaughtErrors() {
    handleUncaughtErrors(Thread.currentThread());
  }

  /**
   * Handle all uncaught errors on {@code thread} with this `Rollbar`
   * @param thread the thread to handle errors on
   */
  public void handleUncaughtErrors(Thread thread) {
    final Rollbar rollbar = this;
    thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      public void uncaughtException(Thread t, Throwable e) {
        rollbar.log(e);
      }
    });
  }

  /**
   * Record a critical error
   * @param error the error
   */
  public void critical(Throwable error) {
    log(error, null, null, Level.CRITICAL);
  }

  /**
   * Record an error
   * @param error the error
   */
  public void error(Throwable error) {
    log(error, null, null, Level.ERROR);
  }

  /**
   * Record an error as a warning
   * @param error the error
   */
  public void warning(Throwable error) {
    log(error, null, null, Level.WARNING);
  }

  /**
   * Record an error as an info
   * @param error the error
   */
  public void info(Throwable error) {
    log(error, null, null, Level.INFO);
  }

  /**
   * Record an error as debugging information
   * @param error the error
   */
  public void debug(Throwable error) {
    log(error, null, null, Level.DEBUG);
  }

  /**
   * Log an error at the default level
   * @param error the error
   */
  public void log(Throwable error) {
    log(error, null, null, null);
  }

  /**
   * Log an error at level specified.
   * @param error the error
   * @param level the level of the error
   */
  public void log(Throwable error, Level level) {
    log(error, null, null, level);
  }

  /**
   * Record a critical error with extra information attached
   * @param error the error
   * @param custom the extra information
   */
  public void critical(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, Level.CRITICAL);
  }

  /**
   * Record an error with extra information attached
   * @param error the error
   * @param custom the extra information
   */
  public void error(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, Level.ERROR);
  }

  /**
   * Record a warning error with extra information attached
   * @param error the error
   * @param custom the extra information
   */
  public void warning(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, Level.WARNING);
  }

  /**
   * Record an info error with extra information attached
   * @param error the error
   * @param custom the extra information
   */
  public void info(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, Level.INFO);
  }

  /**
   * Record a debug error with extra information attached
   * @param error the error
   * @param custom the extra information
   */
  public void debug(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, Level.DEBUG);
  }

  /**
   * Record an error with extra information attached at the default level
   * @param error the error
   * @param custom the extra information
   */
  public void log(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, null);
  }

  /**
   * Record an error with extra information attached at the level specified
   * @param error the error
   * @param custom the extra information
   * @param level the level
   */
  public void log(Throwable error, Map<String, Object> custom, Level level) {
    log(error, custom, null, level);
  }

  /**
   * Record a critical error with human readable description
   * @param error the error
   * @param description human readable description of error
   */
  public void critical(Throwable error, String description) {
    log(error, null, description, Level.CRITICAL);
  }

  /**
   * Record an error with human readable description
   * @param error the error
   * @param description human readable description of error
   */
  public void error(Throwable error, String description) {
    log(error, null, description, Level.ERROR);
  }

  /**
   * Record a warning with human readable description
   * @param error the error
   * @param description human readable description of error
   */
  public void warning(Throwable error, String description) {
    log(error, null, description, Level.WARNING);
  }

  /**
   * Record an info error with human readable description
   * @param error the error
   * @param description human readable description of error
   */
  public void info(Throwable error, String description) {
    log(error, null, description, Level.INFO);
  }

  /**
   * Record a debug error with human readable description
   * @param error the error
   * @param description human readable description of error
   */
  public void debug(Throwable error, String description) {
    log(error, null, description, Level.DEBUG);
  }

  /**
   * Record an error with human readable description at the default level
   * @param error the error
   * @param description human readable description of error
   */
  public void log(Throwable error, String description) {
    log(error, null, description, null);
  }

  /**
   * Record a debug error with human readable description at the specified level
   * @param error the error
   * @param description human readable description of error
   * @param level the level
   */
  public void log(Throwable error, String description, Level level) {
    log(error, null, description, level);
  }

  /**
   * Record a critical error with custom parameters and human readable description
   * @param error the error
   * @param custom the custom data
   * @param description the human readable description of error
   */
  public void critical(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.CRITICAL);
  }

  /**
   * Record an error with custom parameters and human readable description
   * @param error the error
   * @param custom the custom data
   * @param description the human readable description of error
   */
  public void error(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.ERROR);
  }

  /**
   * Record a warning error with custom parameters and human readable description
   * @param error the error
   * @param custom the custom data
   * @param description the human readable description of error
   */
  public void warning(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.WARNING);
  }

  /**
   * Record an info error with custom parameters and human readable description
   * @param error the error
   * @param custom the custom data
   * @param description the human readable description of error
   */
  public void info(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.INFO);
  }

  /**
   * Record a debug error with custom parameters and human readable description
   * @param error the error
   * @param custom the custom data
   * @param description the human readable description of error
   */
  public void debug(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.DEBUG);
  }

  /**
   * Record an error with custom parameters and human readable description at the default level
   * @param error the error
   * @param custom the custom data
   * @param description the human readable description of error
   */
  public void log(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, null);
  }

  /**
   * Record a critical message
   * @param message the message
   */
  public void critical(String message) {
    log(null, null, message, Level.CRITICAL);
  }

  /**
   * Record an error message
   * @param message the message
   */
  public void error(String message) {
    log(null, null, message, Level.ERROR);
  }

  /**
   * Record a warning message
   * @param message the message
   */
  public void warning(String message) {
    log(null, null, message, Level.WARNING);
  }

  /**
   * Record an informational message
   * @param message the message
   */
  public void info(String message) {
    log(null, null, message, Level.INFO);
  }

  /**
   * Record a debugging message
   * @param message the message
   */
  public void debug(String message) {
    log(null, null, message, Level.DEBUG);
  }

  /**
   * Record a debugging message at the default level of WARNING
   * @param message the message
   */
  public void log(String message) {
    log(null, null, message, null);
  }

  /**
   * Record a message at the level specified
   * @param message the message
   * @param level the level
   */
  public void log(String message, Level level) {
    log(null, null, message, level);
  }

  /**
   * Record a critical message with extra information attached
   * @param message the message
   * @param custom the extra information
   */
  public void critical(String message, Map<String, Object> custom) {
    log(null, custom, message, Level.CRITICAL);
  }

  /**
   * Record a error message with extra information attached
   * @param message the message
   * @param custom the extra information
   */
  public void error(String message, Map<String, Object> custom) {
    log(null, custom, message, Level.ERROR);
  }

  /**
   * Record a warning message with extra information attached
   * @param message the message
   * @param custom the extra information
   */
  public void warning(String message, Map<String, Object> custom) {
    log(null, custom, message, Level.WARNING);
  }

  /**
   * Record an informational message with extra information attached
   * @param message the message
   * @param custom the extra information
   */
  public void info(String message, Map<String, Object> custom) {
    log(null, custom, message, Level.INFO);
  }

  /**
   * Record a debugging message with extra information attached
   * @param message the message
   * @param custom the extra information
   */
  public void debug(String message, Map<String, Object> custom) {
    log(null, custom, message, Level.DEBUG);
  }

  /**
   * Record a message with extra information attached at the default level of WARNING
   * @param message the message
   * @param custom the extra information
   */
  public void log(String message, Map<String, Object> custom) {
    log(null, custom, message, null);
  }

  /**
   * Record a message with extra information attached at the specified level
   * @param message the message
   * @param custom the extra information
   * @param level the level
   */
  public void log(String message, Map<String, Object> custom, Level level) {
    log(null, custom, message, level);
  }

  /**
   * Record an error or message with extra data at the level specified. At least ene of `error` or `description` must
   * be non-null. If error is null, `description` will be sent as a message. If error is non-null, description will be
   * sent as the description of the error.
   * Custom data will be attached to message if the error is null.
   * @param error the error (if any)
   * @param custom the custom data (if any)
   * @param description the description of the error, or the message to send
   * @param level the level to send it at
   */
  public void log(Throwable error, Map<String, Object> custom, String description, Level level) {
    this.rollbar.log(error, custom, description, level);
  }
}
