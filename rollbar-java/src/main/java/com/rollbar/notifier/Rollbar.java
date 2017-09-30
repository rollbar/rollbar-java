package com.rollbar.notifier;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.uncaughtexception.RollbarUncaughtExceptionHandler;
import com.rollbar.notifier.util.BodyFactory;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This is the current Rollbar notifier and the main starting point to send the data to Rollbar.
 */
public class Rollbar {

  private static volatile Rollbar notifier;

  private Config config;

  private BodyFactory bodyFactory;

  /**
   * Constructor.
   *
   * @param config the configuration used by the notifier.
   */
  public Rollbar(Config config) {
    this(config, new BodyFactory());
  }

  private Rollbar(Config config, BodyFactory bodyFactory) {
    this.config = config;
    this.bodyFactory = bodyFactory;

    if (config.handleUncaughtErrors()) {
      this.handleUncaughtErrors();
    }
  }

  /**
   * Method to initialize the library managed notifier instance.
   *
   * @param config the configuration.
   * @return the library managed instance.
   */
  public static Rollbar init(Config config) {
    if (notifier == null) {

      synchronized (Rollbar.class) {
        if (notifier == null) {
          notifier = new Rollbar(config);
        }
      }
    }

    return notifier;
  }

  /**
   * Handle all uncaught errors on current thread with this `Rollbar`.
   */
  public void handleUncaughtErrors() {
    handleUncaughtErrors(Thread.currentThread());
  }

  /**
   * Handle all uncaught errors on {@code thread} with this `Rollbar`.
   *
   * @param thread the thread to handle errors on.
   */
  public void handleUncaughtErrors(Thread thread) {
    Objects.requireNonNull(thread, "thread");

    UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    thread.setUncaughtExceptionHandler(new RollbarUncaughtExceptionHandler(this,
        uncaughtExceptionHandler));
  }

  /**
   * Record a critical error.
   *
   * @param error the error.
   */
  public void critical(Throwable error) {
    critical(error, null, null);
  }

  /**
   * Record a critical error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public void critical(Throwable error, String description) {
    critical(error, null, description);
  }

  /**
   * Record a critical error with extra information attached.
   *
   * @param error the error.
   * @param custom the extra information.
   */
  public void critical(Throwable error, Map<String, Object> custom) {
    critical(error, custom, null);
  }

  /**
   * Record a critical message.
   *
   * @param message the message.
   */
  public void critical(String message) {
    critical(null, null, message);
  }

  /**
   * Record a critical message with extra information attached.
   *
   * @param message the message.
   * @param custom the extra information.
   */
  public void critical(String message, Map<String, Object> custom) {
    critical(null, custom, message);
  }

  /**
   * Record a critical error with custom parameters and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public void critical(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.CRITICAL);
  }

  /**
   * Record an error.
   *
   * @param error the error.
   */
  public void error(Throwable error) {
    error(error, null, null);
  }

  /**
   * Record an error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public void error(Throwable error, String description) {
    error(error, null, description);
  }

  /**
   * Record an error with extra information attached.
   *
   * @param error the error.
   * @param custom the extra information.
   */
  public void error(Throwable error, Map<String, Object> custom) {
    error(error, custom, null);
  }

  /**
   * Record an error message.
   *
   * @param message the message.
   */
  public void error(String message) {
    error(null, null, message);
  }

  /**
   * Record a error message with extra information attached.
   *
   * @param message the message.
   * @param custom the extra information.
   */
  public void error(String message, Map<String, Object> custom) {
    error(null, custom, message);
  }

  /**
   * Record an error with custom parameters and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public void error(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.ERROR);
  }

  /**
   * Record an error as a warning.
   *
   * @param error the error.
   */
  public void warning(Throwable error) {
    warning(error, null, null);
  }

  /**
   * Record a warning with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public void warning(Throwable error, String description) {
    warning(error, null, description);
  }

  /**
   * Record a warning error with extra information attached.
   *
   * @param error the error.
   * @param custom the extra information.
   */
  public void warning(Throwable error, Map<String, Object> custom) {
    warning(error, custom, null);
  }

  /**
   * Record a warning message.
   *
   * @param message the message.
   */
  public void warning(String message) {
    warning(null, null, message);
  }

  /**
   * Record a warning message with extra information attached.
   *
   * @param message the message.
   * @param custom the extra information.
   */
  public void warning(String message, Map<String, Object> custom) {
    warning(null, custom, message);
  }

  /**
   * Record a warning error with custom parameters and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public void warning(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.WARNING);
  }

  /**
   * Record an error as an info.
   *
   * @param error the error.
   */
  public void info(Throwable error) {
    info(error, null, null);
  }

  /**
   * Record an info error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public void info(Throwable error, String description) {
    info(error, null, description);
  }

  /**
   * Record an info error with extra information attached.
   *
   * @param error the error.
   * @param custom the extra information.
   */
  public void info(Throwable error, Map<String, Object> custom) {
    info(error, custom, null);
  }

  /**
   * Record an informational message.
   *
   * @param message the message.
   */
  public void info(String message) {
    info(null, null, message);
  }

  /**
   * Record an informational message with extra information attached.
   *
   * @param message the message.
   * @param custom the extra information.
   */
  public void info(String message, Map<String, Object> custom) {
    info(null, custom, message);
  }

  /**
   * Record an info error with custom parameters and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public void info(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.INFO);
  }

  /**
   * Record an error as debugging information.
   *
   * @param error the error.
   */
  public void debug(Throwable error) {
    debug(error, null, null);
  }

  /**
   * Record a debug error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public void debug(Throwable error, String description) {
    debug(error, null, description);
  }

  /**
   * Record a debug error with extra information attached.
   *
   * @param error the error.
   * @param custom the extra information.
   */
  public void debug(Throwable error, Map<String, Object> custom) {
    debug(error, custom, null);
  }

  /**
   * Record a debugging message.
   *
   * @param message the message.
   */
  public void debug(String message) {
    debug(null, null, message);
  }

  /**
   * Record a debugging message with extra information attached.
   *
   * @param message the message.
   * @param custom the extra information.
   */
  public void debug(String message, Map<String, Object> custom) {
    debug(null, custom, message);
  }

  /**
   * Record a debug error with custom parameters and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public void debug(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.DEBUG);
  }

  /**
   * Log an error at the level returned by {@link Rollbar#level}.
   *
   * @param error the error.
   */
  public void log(Throwable error) {
    log(error, null, null, null);
  }

  /**
   * Record an error with human readable description at the default level returned by {@link
   * Rollbar#level}.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public void log(Throwable error, String description) {
    log(error, null, description, null);
  }

  /**
   * Record an error with extra information attached at the default level returned by {@link
   * Rollbar#level}.
   *
   * @param error the error.
   * @param custom the extra information.
   */
  public void log(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, null);
  }

  /**
   * Record an error with extra information attached at the level specified.
   *
   * @param error the error.
   * @param custom the extra information.
   * @param level the level.
   */
  public void log(Throwable error, Map<String, Object> custom, Level level) {
    log(error, custom, null, level);
  }

  /**
   * Log an error at level specified.
   *
   * @param error the error.
   * @param level the level of the error.
   */
  public void log(Throwable error, Level level) {
    log(error, null, null, level);
  }

  /**
   * Record a debug error with human readable description at the specified level.
   *
   * @param error the error.
   * @param description human readable description of error.
   * @param level the level.
   */
  public void log(Throwable error, String description, Level level) {
    log(error, null, description, level);
  }

  /**
   * Record an error with custom parameters and human readable description at the default level
   * returned by {@link Rollbar#level}.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public void log(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, null);
  }

  /**
   * Record a debugging message at the level returned by {@link Rollbar#level} (WARNING unless level
   * is overridden).
   *
   * @param message the message.
   */
  public void log(String message) {
    log(null, null, message, null);
  }

  /**
   * Record a message with extra information attached at the default level returned by {@link
   * Rollbar#level}, (WARNING unless level overridden).
   *
   * @param message the message.
   * @param custom the extra information.
   */
  public void log(String message, Map<String, Object> custom) {
    log(null, custom, message, null);
  }

  /**
   * Record a message at the level specified.
   *
   * @param message the message.
   * @param level the level.
   */
  public void log(String message, Level level) {
    log(null, null, message, level);
  }

  /**
   * Record a message with extra information attached at the specified level.
   *
   * @param message the message.
   * @param custom the extra information.
   * @param level the level.
   */
  public void log(String message, Map<String, Object> custom, Level level) {
    log(null, custom, message, level);
  }

  /**
   * Record an error or message with extra data at the level specified. At least ene of `error` or
   * `description` must be non-null. If error is null, `description` will be sent as a message. If
   * error is non-null, description will be sent as the description of the error. Custom data will
   * be attached to message if the error is null. Custom data will extend whatever {@link
   * Config#custom} returns.
   *
   * @param error the error (if any).
   * @param custom the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level the level to send it at.
   */
  public void log(Throwable error, Map<String, Object> custom, String description, Level level) {
    process(error, custom, description, level);
  }

  /**
   * Get the level of the error or message. By default: CRITICAL for {@link Error}, ERROR for other
   * {@link Throwable}, WARNING for messages. Override to change this default.
   *
   * @param error the error.
   * @return the level.
   */
  public Level level(Throwable error) {
    if (error == null) {
      return Level.WARNING;
    }
    if (error instanceof Error) {
      return Level.CRITICAL;
    }
    return Level.ERROR;
  }

  private void process(Throwable error, Map<String, Object> custom, String description,
      Level level) {

    // Pre filter
    if (config.filter() != null && config.filter().preProcess(level, error, custom, description)) {
      return;
    }

    // Gather information to build a payload.
    Data data = buildData(error, custom, description, level);

    // Tranform the data
    if (config.transformer() != null) {
      data = config.transformer().transform(data);
    }

    // Append if needed uuid or fingerprint data.
    if (config.uuidGenerator() != null || config.fingerPrintGenerator() != null) {
      Data.Builder dataBuilder = new Data.Builder(data);

      // UUID
      if (config.uuidGenerator() != null) {
        dataBuilder.uuid(config.uuidGenerator().from(data));
      }

      // Fingerprint
      if (config.fingerPrintGenerator() != null) {
        dataBuilder.fingerprint(config.fingerPrintGenerator().from(data));
      }
      data = dataBuilder.build();
    }

    // Post filter
    if (config.filter() != null && config.filter().postProcess(data)) {
      return;
    }

    // Payload
    Payload payload = new Payload.Builder()
        .accessToken(config.accessToken())
        .data(data).build();

    // Send
    sendPayload(payload);
  }

  private Data buildData(Throwable error, Map<String, Object> custom, String description,
      Level level) {

    Data.Builder dataBuilder = new Data.Builder()
        .environment(config.environment())
        .codeVersion(config.codeVersion())
        .platform(config.platform())
        .language(config.language())
        .framework(config.framework())
        .level(level != null ? level : level(error))
        .body(bodyFactory.from(error, description));

    // Gather data from providers.

    // Context
    if (config.context() != null) {
      dataBuilder.context(config.context().provide());
    }

    // Request
    if (config.request() != null) {
      dataBuilder.request(config.request().provide());
    }

    // Person
    if (config.person() != null) {
      dataBuilder.person(config.person().provide());

    }

    // Server
    if (config.server() != null) {
      dataBuilder.server(config.server().provide());
    }

    // Client
    if (config.client() != null) {
      dataBuilder.client(config.client().provide());
    }

    // Custom
    Map<String, Object> tmpCustom = new HashMap<>();
    if (config.custom() != null) {
      tmpCustom.putAll(config.custom().provide());
    }
    if (custom != null) {
      custom.putAll(custom);
    }
    if (tmpCustom.size() > 0) {
      dataBuilder.custom(tmpCustom);
    }

    // Notifier
    if (config.notifier() != null) {
      dataBuilder.notifier(config.notifier().provide());
    }

    return dataBuilder.build();
  }

  private void sendPayload(Payload payload) {
    if (config.sender() != null) {
      config.sender().send(payload);
    }
  }
}
