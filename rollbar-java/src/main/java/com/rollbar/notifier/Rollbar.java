package com.rollbar.notifier;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.notifier.uncaughtexception.RollbarUncaughtExceptionHandler;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.notifier.util.ObjectsUtils;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the current Rollbar notifier and the main starting point to send the data to Rollbar.
 */
public class Rollbar extends RollbarBase<Void, Config> {

  private static Logger LOGGER = LoggerFactory.getLogger(Rollbar.class);

  private static final Void VOID = null;

  private static volatile Rollbar notifier;

  /**
   * Constructor.
   *
   * @param config the configuration used by the notifier.
   */
  public Rollbar(Config config) {
    this(config, new BodyFactory());
  }

  Rollbar(Config config, BodyFactory bodyFactory) {
    super(config, bodyFactory, VOID);

    if (config.handleUncaughtErrors()) {
      this.handleUncaughtErrors();
    }
    processAppPackages(config);
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
          LOGGER.debug("Rollbar managed notifier created.");
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
    ObjectsUtils.requireNonNull(thread, "thread");
    LOGGER.debug("Handling uncaught errors for thread: {}.", thread);
    UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    thread.setUncaughtExceptionHandler(new RollbarUncaughtExceptionHandler(this,
        uncaughtExceptionHandler));
  }

  /**
   * Replace the configuration of this instance.
   * This {@link ConfigBuilder} passed to configProvider is
   * preconfigured with the values of the current configuration.
   * This method potentially blocks to acquire a locks when
   * safely work with the configuration.
   *
   * @param configProvider the provider of a new configuration
   */
  public void configure(ConfigProvider configProvider) {
    ConfigBuilder builder;

    this.configReadLock.lock();
    try {
      builder = ConfigBuilder.withConfig(this.config);
    } finally {
      this.configReadLock.unlock();
    }

    Config newConfig = configProvider.provide(builder);

    this.configure(newConfig);
  }

  /**
   * Replace the configuration of this instance directly.
   *
   * @param config the new configuration.
   */
  public void configure(Config config) {
    super.configure(config);
  }

  /**
   * Get the level of the error or message. The Config passed in contains the defaults
   * to use for the cases of an Error, Throwable, or a Message. The default in the Config
   * if otherwise left unspecified is: CRITICAL for {@link Error}, ERROR for other
   * {@link Throwable}, WARNING for messages. Use the methods on ConfigBuilder to
   * change these defaults
   *
   * @param config the current Config.
   * @param error  the error.
   * @return the level.
   */
  public Level level(Config config, Throwable error) {
    return super.level(config, error);
  }

  /**
   * Get the current config.
   *
   * @return the config.
   */
  public Config config() {
    return config;
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
   * Record a critical error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
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
   * Record a critical message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public void critical(String message, Map<String, Object> custom) {
    critical(null, custom, message);
  }

  /**
   * Record a critical error with custom data and human readable description.
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
   * Record an error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
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
   * Record a error message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public void error(String message, Map<String, Object> custom) {
    error(null, custom, message);
  }

  /**
   * Record an error with custom data and human readable description.
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
   * Record a warning error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
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
   * Record a warning message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public void warning(String message, Map<String, Object> custom) {
    warning(null, custom, message);
  }

  /**
   * Record a warning error with data and human readable description.
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
   * Record an info error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
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
   * Record an informational message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public void info(String message, Map<String, Object> custom) {
    info(null, custom, message);
  }

  /**
   * Record an info error with custom data and human readable description.
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
   * Record a debug error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
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
   * Record a debugging message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public void debug(String message, Map<String, Object> custom) {
    debug(null, custom, message);
  }

  /**
   * Record a debug error with custom data and human readable description.
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
   * Record an error with custom data attached at the default level returned by {@link
   * Rollbar#level}.
   *
   * @param error the error.
   * @param custom the custom data.
   */
  public void log(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, null);
  }

  /**
   * Record an error with custom data attached at the level specified.
   *
   * @param error the error.
   * @param custom the custom data.
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
   * Record an error with custom data and human readable description at the default level
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
   * Record a message with custom data attached at the default level returned by {@link
   * Rollbar#level}, (WARNING unless level overridden).
   *
   * @param message the message.
   * @param custom the custom data.
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
   * Record a message with custom data attached at the specified level.
   *
   * @param message the message.
   * @param custom the custom data.
   * @param level the level.
   */
  public void log(String message, Map<String, Object> custom, Level level) {
    log(null, custom, message, level);
  }

  /**
   * Record an error or message with extra data at the level specified. At least one of `error` or
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
    log(error, custom, description, level, false);
  }

  /**
   * Record an error or message with extra data at the level specified. At least one of `error` or
   * `description` must be non-null. If error is null, `description` will be sent as a message. If
   * error is non-null, description will be sent as the description of the error. Custom data will
   * be attached to message if the error is null. Custom data will extend whatever {@link
   * Config#custom} returns.
   *
   * @param error the error (if any).
   * @param custom the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level the level to send it at.
   * @param isUncaught whether this data comes from an uncaught exception.
   */
  public void log(Throwable error, Map<String, Object> custom, String description, Level level,
      boolean isUncaught) {
    this.log(wrapThrowable(error), custom, description, level, isUncaught);
  }

  /**
   * Record an error or message with extra data at the level specified. At least one of `error` or
   * `description` must be non-null. If error is null, `description` will be sent as a message. If
   * error is non-null, description will be sent as the description of the error. Custom data will
   * be attached to message if the error is null. Custom data will extend whatever {@link
   * Config#custom} returns.
   *
   * @param error the error (if any).
   * @param thread the thread where the error happened (if any).
   * @param custom the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level the level to send it at.
   * @param isUncaught whether this data comes from an uncaught exception.
   */
  public void log(
      Throwable error,
      Thread thread,
      Map<String, Object> custom,
      String description,
      Level level,
      boolean isUncaught
  ) {
    this.log(wrapThrowable(error, thread), custom, description, level, isUncaught);
  }

  /**
   * Record an error or message with extra data at the level specified. At least one of `error` or
   * `description` must be non-null. If error is null, `description` will be sent as a message. If
   * error is non-null, description will be sent as the description of the error. Custom data will
   * be attached to message if the error is null. Custom data will extend whatever {@link
   * Config#custom} returns.
   *
   * @param error the error (if any).
   * @param custom the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level the level to send it at.
   * @param isUncaught whether this data comes from an uncaught exception.
   */
  public void log(ThrowableWrapper error, Map<String, Object> custom, String description,
                  Level level, boolean isUncaught) {
    try {
      process(error, custom, description, level, isUncaught);
    } catch (Exception e) {
      LOGGER.error("Error while processing payload to send to Rollbar: {}", e);
    }
  }

  public void close(boolean wait) throws Exception {
    this.config.sender().close(wait);
  }

  /**
   * Send JSON payload.
   *
   * @param json the json payload.
   */
  public void sendJsonPayload(String json) {
    try {
      this.configReadLock.lock();
      Config config = this.config;
      this.configReadLock.unlock();

      sendPayload(config, new Payload(json));
    } catch (Exception e) {
      LOGGER.error("Error while sending payload to Rollbar: {}", e);
    }
  }

  @Override
  protected Void sendPayload(Config config, Payload payload) {
    if (config.sender() != null) {
      LOGGER.debug("Sending payload.");
      config.sender().send(payload);
    }

    return VOID;
  }
}
