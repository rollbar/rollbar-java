package com.rollbar.notifier;

import com.rollbar.api.annotations.Unstable;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.jvmti.ThrowableCache;
import com.rollbar.notifier.config.CommonConfig;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common logic for synchronous and non-blocking async notifiers.
 *
 * @param <RESULT> The result type of a send operation. Sync methods can use {@link Void}
 * @param <C> The configuration class for the notifier
 */
@Unstable
public abstract class RollbarBase<RESULT, C extends CommonConfig> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RollbarBase.class);

  protected BodyFactory bodyFactory;

  protected C config;

  protected final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  protected final Lock configReadLock = configReadWriteLock.readLock();
  protected final Lock configWriteLock = configReadWriteLock.writeLock();
  private final RESULT emptyResult;

  protected RollbarBase(C config, BodyFactory bodyFactory, RESULT emptyResult) {
    this.config = config;
    this.bodyFactory = bodyFactory;
    this.emptyResult = emptyResult;
  }

  /**
   * Replace the configuration of this instance directly.
   *
   * @param config the new configuration.
   */
  protected void configure(C config) {
    LOGGER.debug("Reloading configuration.");
    this.configWriteLock.lock();
    try {
      this.config = config;
      processAppPackages(config);
    } finally {
      this.configWriteLock.unlock();
    }
  }

  protected void processAppPackages(CommonConfig config) {
    for (String appPackage : config.appPackages()) {
      ThrowableCache.addAppPackage(appPackage);
    }
  }

  /**
   * Get the level of the error or message.
   *
   * @param config the current Config.
   * @param error  the error.
   * @return the level.
   */
  protected Level level(CommonConfig config, Throwable error) {
    if (error == null) {
      return config.defaultMessageLevel();
    }
    if (error instanceof Error) {
      return config.defaultErrorLevel();
    }
    return config.defaultThrowableLevel();
  }

  private Level getOccurrenceLevel(CommonConfig config, ThrowableWrapper error,
                                   Level levelOverride) {
    if (levelOverride != null) {
      return levelOverride;
    }

    Throwable throwable = error == null ? null : error.getThrowable();

    return level(config, throwable);
  }

  protected Data buildData(CommonConfig config, ThrowableWrapper error, Map<String, Object> custom,
                           String description, Level level, boolean isUncaught) {

    Data.Builder dataBuilder = new Data.Builder()
        .environment(config.environment())
        .codeVersion(config.codeVersion())
        .platform(config.platform())
        .language(config.language())
        .framework(config.framework())
        .level(getOccurrenceLevel(config, error, level))
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

  protected RESULT process(ThrowableWrapper error, Map<String, Object> custom, String description,
                           Level level, boolean isUncaught) {
    C config;

    this.configReadLock.lock();
    try {
      config = this.config;
    } finally {
      this.configReadLock.unlock();
    }

    if (!config.isEnabled()) {
      LOGGER.debug("Notifier disabled.");
      return emptyResult;
    }

    // Pre filter
    if (config.filter() != null && config.filter().preProcess(level,
            error != null ? error.getThrowable() : null, custom, description)) {
      LOGGER.debug("Pre-filtered error: {}", error);
      return emptyResult;
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
      return emptyResult;
    }

    // Payload
    Payload payload = new Payload.Builder()
        .accessToken(config.accessToken())
        .data(data).build();

    LOGGER.debug("Payload built: {}", payload);

    // Send
    return sendPayload(config, payload);
  }

  protected RollbarThrowableWrapper wrapThrowable(Throwable error) {
    RollbarThrowableWrapper rollbarThrowableWrapper = null;

    if (error != null) {
      rollbarThrowableWrapper = new RollbarThrowableWrapper(error);
    }

    return rollbarThrowableWrapper;
  }

  protected abstract RESULT sendPayload(C config, Payload payload);
}
