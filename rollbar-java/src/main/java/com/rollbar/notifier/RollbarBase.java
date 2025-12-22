package com.rollbar.notifier;

import com.rollbar.api.annotations.Unstable;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.jvmti.ThrowableCache;
import com.rollbar.notifier.config.CommonConfig;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;
import com.rollbar.notifier.truncation.PayloadTruncator;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.notifier.util.ObjectsUtils;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;

import java.util.HashMap;
import java.util.List;
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
  private static final int MAX_PAYLOAD_SIZE_BYTES = 512 * 1024; // 512kb

  protected BodyFactory bodyFactory;
  protected PayloadTruncator payloadTruncator;

  protected C config;

  protected final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  protected final Lock configReadLock = configReadWriteLock.readLock();
  protected final Lock configWriteLock = configReadWriteLock.writeLock();
  private final RESULT emptyResult;
  private final TelemetryEventTracker telemetryEventTracker;

  protected RollbarBase(C config, BodyFactory bodyFactory, RESULT emptyResult) {
    this.config = config;
    configureTruncation(config);
    this.bodyFactory = bodyFactory;
    this.emptyResult = emptyResult;
    this.telemetryEventTracker = config.telemetryEventTracker();
  }

  /**
   * Record log telemetry event. ({@link TelemetryType#LOG}).
   *
   * @param level   the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param message the message sent for this event (e.g. "hello world").
   */
  public void recordLogEventFor(Level level, String message) {
    telemetryEventTracker.recordLogEventFor(level, getSource(), message);
  }

  /**
   * Record manual telemetry event. ({@link TelemetryType#MANUAL})
   *
   * @param level   the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param message the message sent for this event (e.g. "hello world").
   */
  public void recordManualEventFor(Level level, String message) {
    telemetryEventTracker.recordManualEventFor(level, getSource(), message);
  }

  /**
   * Record navigation telemetry event with from (origin) and to (destination).
   * ({@link TelemetryType#NAVIGATION}).
   *
   * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param from  the starting point (e.g. "SettingView").
   * @param to    the destination point (e.g. "HomeView").
   */
  public void recordNavigationEventFor(Level level, String from, String to) {
    telemetryEventTracker.recordNavigationEventFor(level, getSource(), from, to);
  }

  /**
   * Record network telemetry event with method, url, and status code.
   * ({@link TelemetryType#NETWORK}).
   *
   * @param level      the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
   * @param method     the verb used (e.g. "POST").
   * @param url        the api url (e.g. "<a href="http://rollbar.com/test/api">
   *                   http://rollbar.com/test/api</a>").
   * @param statusCode the response status code (e.g. "404").
   */
  public void recordNetworkEventFor(Level level, String method, String url, String statusCode) {
    telemetryEventTracker.recordNetworkEventFor(level, getSource(), method, url, statusCode);
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
      configureTruncation(config);
      processAppPackages(config);
    } finally {
      this.configWriteLock.unlock();
    }
  }

  private void configureTruncation(C config) {
    if (config.truncateLargePayloads()) {
      ObjectsUtils.requireNonNull(config.jsonSerializer(),
          "A JSON serializer is required when performing payload truncation.");
      this.payloadTruncator = new PayloadTruncator(config.jsonSerializer());
    } else {
      this.payloadTruncator = null;
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
        .body(makeBody(error, description))
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

    payload = truncateIfNecessary(config, payload);

    // Send
    return sendPayload(config, payload);
  }

  private Payload truncateIfNecessary(C config, Payload payload) {
    boolean doTruncate = config.truncateLargePayloads();
    PayloadTruncator truncator = this.payloadTruncator;

    if (doTruncate && truncator != null) {
      PayloadTruncator.PayloadTruncationResult result =
          truncator.truncate(payload, MAX_PAYLOAD_SIZE_BYTES);
      payload = result.getPayload();
      if (result.finalSize > MAX_PAYLOAD_SIZE_BYTES) {
        LOGGER.warn("Sending payload with size " + result.finalSize + " bytes, "
            + "which is over the limit of " + MAX_PAYLOAD_SIZE_BYTES + " bytes");
      }
    }

    return payload;
  }

  protected RollbarThrowableWrapper wrapThrowable(Throwable error, Thread thread) {
    if (error != null && thread != null) {
      return new RollbarThrowableWrapper(error, thread);
    } else {
      return wrapThrowable(error);
    }
  }

  protected RollbarThrowableWrapper wrapThrowable(Throwable error) {
    if (error == null) {
      return null;
    }

    return new RollbarThrowableWrapper(error);
  }

  protected abstract RESULT sendPayload(C config, Payload payload);

  private Body makeBody(ThrowableWrapper error, String description) {
    List<TelemetryEvent> telemetryEvents = telemetryEventTracker.getAll();
    if (telemetryEvents.isEmpty()) {
      return bodyFactory.from(error, description);
    }
    return bodyFactory.from(error, description, telemetryEvents);
  }

  private Source getSource() {
    String platform = config.platform();
    if ("android".equals(platform)) {
      return Source.CLIENT;
    } else {
      return Source.SERVER;
    }
  }
}
