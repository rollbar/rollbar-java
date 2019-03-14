package com.rollbar.notifier;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.jvmti.ThrowableCache;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.notifier.uncaughtexception.RollbarUncaughtExceptionHandler;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.notifier.util.ObjectsUtils;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the current Rollbar notifier and the main starting point to send the data to Rollbar.
 */
public class Rollbar {

  private static Logger LOGGER = LoggerFactory.getLogger(Rollbar.class);

  private static volatile Rollbar notifier;

  private Config config;

  private final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  private final Lock configReadLock = configReadWriteLock.readLock();
  private final Lock configWriteLock = configReadWriteLock.writeLock();

  private BodyFactory bodyFactory;

  /**
   * Constructor.
   *
   * @param config the configuration used by the notifier.
   */
  public Rollbar(Config config) {
    this(config, new BodyFactory());
  }

  Rollbar(Config config, BodyFactory bodyFactory) {
    this.config = config;
    this.bodyFactory = bodyFactory;

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
    LOGGER.debug("Reloading configuration.");
    this.configWriteLock.lock();
    try {
      this.config = config;
      processAppPackages(config);
    } finally {
      this.configWriteLock.unlock();
    }
  }

  private void processAppPackages(Config config) {
    for (String appPackage : config.appPackages()) {
      ThrowableCache.addAppPackage(appPackage);
    }
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
    log(error, custom, description, level, false);
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
   * @param isUncaught whether or not this data comes from an uncaught exception.
   */
  public void log(Throwable error, Map<String, Object> custom, String description, Level level,
      boolean isUncaught) {
    RollbarThrowableWrapper rollbarThrowableWrapper = null;

    if (error != null) {
      rollbarThrowableWrapper = new RollbarThrowableWrapper(error);

    }
    this.log(rollbarThrowableWrapper, custom, description, level, isUncaught);
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
   * @param isUncaught whether or not this data comes from an uncaught exception.
   */
  public void log(ThrowableWrapper error, Map<String, Object> custom, String description,
                  Level level, boolean isUncaught) {
    try {
      process(error, custom, description, level, isUncaught);
    } catch (Exception e) {
      LOGGER.error("Error while processing payload to send to Rollbar: {}", e);
    }
  }

  /**
   * Get the level of the error or message. The Config passed in contains the defaults
   * to use for the cases of an Error, Throwable, or a Message. The default in the Config
   * if otherwise left unspecified is: CRITICAL for {@link Error}, ERROR for other
   * {@link Throwable}, WARNING for messages. Use the methods on ConfigBuilder to
   * change these defaults
   *
   * @param config the current Config.
   * @param error the error.
   * @return the level.
   */
  public Level level(Config config, Throwable error) {
    if (error == null) {
      return config.defaultMessageLevel();
    }
    if (error instanceof Error) {
      return config.defaultErrorLevel();
    }
    return config.defaultThrowableLevel();
  }

  public void close(boolean wait) throws Exception {
    this.config.sender().close(wait);
  }

  private void process(ThrowableWrapper error, Map<String, Object> custom, String description,
      Level level, boolean isUncaught) {
    this.configReadLock.lock();
    Config config = this.config;
    this.configReadLock.unlock();

    if (!config.isEnabled()) {
      LOGGER.debug("Notifier disabled.");
      return;
    }
    
    // Pre filter
    if (config.filter() != null && config.filter().preProcess(level, error.getThrowable(), custom,
        description)) {
      LOGGER.debug("Pre-filtered error: {}", error);
      return;
    }

    LOGGER.debug("Gathering information to build the payload.");
    // Gather information to build a payload.
    Data data = buildData(config, error, custom, description, level, isUncaught);

    // Transform the data
    if (config.transformer() != null) {
      LOGGER.debug("Transforming the data.");
      data = config.transformer().transform(data);
    }

    // Append if needed uuid or fingerprint data.
    if (config.uuidGenerator() != null || config.fingerPrintGenerator() != null) {
      Data.Builder dataBuilder = new Data.Builder(data);

      // UUID
      if (config.uuidGenerator() != null) {
        LOGGER.debug("Generating UUID.");
        dataBuilder.uuid(config.uuidGenerator().from(data));
      }

      // Fingerprint
      if (config.fingerPrintGenerator() != null) {
        LOGGER.debug("Generating fingerprint.");
        dataBuilder.fingerprint(config.fingerPrintGenerator().from(data));
      }
      data = dataBuilder.build();
    }

    // Post filter
    if (config.filter() != null && config.filter().postProcess(data)) {
      LOGGER.debug("Post-filtered error: {}", error);
      return;
    }

    // Payload
    Payload payload = new Payload.Builder()
        .accessToken(config.accessToken())
        .data(data).build();

    LOGGER.debug("Payload built: {}", payload);

    // Send
    sendPayload(config, payload);
  }

  private Data buildData(Config config, ThrowableWrapper error, Map<String, Object> custom,
      String description, Level level, boolean isUncaught) {

    Data.Builder dataBuilder = new Data.Builder()
        .environment(config.environment())
        .codeVersion(config.codeVersion())
        .platform(config.platform())
        .language(config.language())
        .framework(config.framework())
        .level(level != null ? level : error != null ? level(config, error.getThrowable())
            : level(config, null))
        .body(bodyFactory.from(error, description))
        .isUncaught(isUncaught);

    // Gather data from providers.

    // Context
    if (config.context() != null) {
      LOGGER.debug("Gathering context info.");
      dataBuilder.context(config.context().provide());
    }

    // Request
    if (config.request() != null) {
      LOGGER.debug("Gathering request info.");
      dataBuilder.request(config.request().provide());
    }

    // Person
    if (config.person() != null) {
      LOGGER.debug("Gathering person info.");
      dataBuilder.person(config.person().provide());

    }

    // Server
    if (config.server() != null) {
      LOGGER.debug("Gathering server info.");
      dataBuilder.server(config.server().provide());
    }

    // Client
    if (config.client() != null) {
      LOGGER.debug("Gathering client info.");
      dataBuilder.client(config.client().provide());
    }

    // Custom
    Map<String, Object> tmpCustom = new HashMap<>();
    if (config.custom() != null) {
      LOGGER.debug("Gathering custom info.");
      Map<String, Object> customProvided = config.custom().provide();
      if (customProvided != null) {
        tmpCustom.putAll(customProvided);
      }
    }
    if (custom != null) {
      tmpCustom.putAll(custom);
    }
    if (tmpCustom.size() > 0) {
      dataBuilder.custom(tmpCustom);
    }

    // Notifier
    if (config.notifier() != null) {
      LOGGER.debug("Gathering notifier info.");
      dataBuilder.notifier(config.notifier().provide());
    }

    // Timestamp
    if (config.timestamp() != null) {
      LOGGER.debug("Gathering timestamp info.");
      dataBuilder.timestamp(config.timestamp().provide());
    }

    return dataBuilder.build();
  }

  private void sendPayload(Config config, Payload payload) {
    if (config.sender() != null) {
      LOGGER.debug("Sending payload.");
      config.sender().send(payload);
    }
  }
}
