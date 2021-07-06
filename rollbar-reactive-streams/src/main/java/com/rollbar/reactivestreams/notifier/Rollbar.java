package com.rollbar.reactivestreams.notifier;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.RollbarBase;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import com.rollbar.reactivestreams.Utils;
import com.rollbar.reactivestreams.notifier.config.Config;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpClient;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is an asynchronous, non-blocking notifier, based on the
 * <a href="https://www.reactive-streams.org/">https://www.reactive-streams.org/</a> specification.
 * </p>
 *
 * <p>
 * It supports most of the same operations as {@link com.rollbar.notifier.Rollbar}, but all IO
 * operations (such as HTTP requests) return a mono reactive-streams {@link Publisher} that will
 * execute the operation asynchronously once a subscription requests at least 1 element.
 * </p>
 *
 * <p>
 *   It requires a non-blocking HTTP client to perform the requests. Built-in support for the
 *   <a href="https://hc.apache.org/httpcomponents-client-5.0.x/">Apache HTTPComponents client</a>
 *   is included if the dependency is available in the classpath. Otherwise an implementation of
 *   {@link AsyncHttpClient} must be provided as part of the configuration. The
 *   rollbar-reactivestreams-reactor project provides a
 *   <a href="https://projectreactor.io/">Project Reactor</a> implementation, but it requires
 *   Java 8 which is why it's provided as a separate package.
 * </p>
 */
public class Rollbar extends RollbarBase<Publisher<Response>, Config>
    implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Rollbar.class);

  private static volatile Rollbar notifier;

  public Rollbar(Config config) {
    this(config, new BodyFactory());
  }

  protected Rollbar(Config config, BodyFactory bodyFactory) {
    super(config, bodyFactory, Utils.<Response>empty());
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

  public void close(boolean wait) throws Exception {
    this.config.asyncSender().close(wait);
  }

  @Override
  public void close() throws Exception {
    this.close(true);
  }

  @Override
  protected Publisher<Response> sendPayload(Config config, Payload payload) {
    return config.asyncSender().send(payload);
  }

  /**
   * Record a critical error.
   *
   * @param error the error.
   */
  public Publisher<Response> critical(Throwable error) {
    return critical(error, null, null);
  }

  /**
   * Record a critical error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public Publisher<Response> critical(Throwable error, String description) {
    return critical(error, null, description);
  }

  /**
   * Record a critical error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
   */
  public Publisher<Response> critical(Throwable error, Map<String, Object> custom) {
    return critical(error, custom, null);
  }

  /**
   * Record a critical message.
   *
   * @param message the message.
   */
  public Publisher<Response> critical(String message) {
    return critical(null, null, message);
  }

  /**
   * Record a critical message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public Publisher<Response> critical(String message, Map<String, Object> custom) {
    return critical(null, custom, message);
  }

  /**
   * Record a critical error with custom data and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public Publisher<Response> critical(Throwable error, Map<String, Object> custom,
                                      String description) {
    return log(error, custom, description, Level.CRITICAL);
  }

  /**
   * Record an error.
   *
   * @param error the error.
   */
  public Publisher<Response> error(Throwable error) {
    return error(error, null, null);
  }

  /**
   * Record an error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public Publisher<Response> error(Throwable error, String description) {
    return error(error, null, description);
  }

  /**
   * Record an error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
   */
  public Publisher<Response> error(Throwable error, Map<String, Object> custom) {
    return error(error, custom, null);
  }

  /**
   * Record an error message.
   *
   * @param message the message.
   */
  public Publisher<Response> error(String message) {
    return error(null, null, message);
  }

  /**
   * Record a error message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public Publisher<Response> error(String message, Map<String, Object> custom) {
    return error(null, custom, message);
  }

  /**
   * Record an error with custom data and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public Publisher<Response> error(Throwable error, Map<String, Object> custom,
                                   String description) {
    return log(error, custom, description, Level.ERROR);
  }

  /**
   * Record an error as a warning.
   *
   * @param error the error.
   */
  public Publisher<Response> warning(Throwable error) {
    return warning(error, null, null);
  }

  /**
   * Record a warning with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public Publisher<Response> warning(Throwable error, String description) {
    return warning(error, null, description);
  }

  /**
   * Record a warning error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
   */
  public Publisher<Response> warning(Throwable error, Map<String, Object> custom) {
    return warning(error, custom, null);
  }

  /**
   * Record a warning message.
   *
   * @param message the message.
   */
  public Publisher<Response> warning(String message) {
    return warning(null, null, message);
  }

  /**
   * Record a warning message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public Publisher<Response> warning(String message, Map<String, Object> custom) {
    return warning(null, custom, message);
  }

  /**
   * Record a warning error with custom data and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public Publisher<Response> warning(Throwable error, Map<String, Object> custom,
                                     String description) {
    return log(error, custom, description, Level.WARNING);
  }

  /**
   * Record an error as an info.
   *
   * @param error the error.
   */
  public Publisher<Response> info(Throwable error) {
    return info(error, null, null);
  }

  /**
   * Record an info error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public Publisher<Response> info(Throwable error, String description) {
    return info(error, null, description);
  }

  /**
   * Record an info error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
   */
  public Publisher<Response> info(Throwable error, Map<String, Object> custom) {
    return info(error, custom, null);
  }

  /**
   * Record an informational message.
   *
   * @param message the message.
   */
  public Publisher<Response> info(String message) {
    return info(null, null, message);
  }

  /**
   * Record an informational message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public Publisher<Response> info(String message, Map<String, Object> custom) {
    return info(null, custom, message);
  }

  /**
   * Record an info error with custom data and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public Publisher<Response> info(Throwable error, Map<String, Object> custom, String description) {
    return log(error, custom, description, Level.INFO);
  }

  /**
   * Record an error as debugging information.
   *
   * @param error the error.
   */
  public Publisher<Response> debug(Throwable error) {
    return debug(error, null, null);
  }

  /**
   * Record a debug error with human readable description.
   *
   * @param error the error.
   * @param description human readable description of error.
   */
  public Publisher<Response> debug(Throwable error, String description) {
    return debug(error, null, description);
  }

  /**
   * Record a debug error with custom data attached.
   *
   * @param error the error.
   * @param custom the custom data.
   */
  public Publisher<Response> debug(Throwable error, Map<String, Object> custom) {
    return debug(error, custom, null);
  }

  /**
   * Record a debugging message.
   *
   * @param message the message.
   */
  public Publisher<Response> debug(String message) {
    return debug(null, null, message);
  }

  /**
   * Record a debugging message with custom data attached.
   *
   * @param message the message.
   * @param custom the custom data.
   */
  public Publisher<Response> debug(String message, Map<String, Object> custom) {
    return debug(null, custom, message);
  }

  /**
   * Record a debug error with custom data and human readable description.
   *
   * @param error the error.
   * @param custom the custom data.
   * @param description the human readable description of error.
   */
  public Publisher<Response> debug(Throwable error, Map<String, Object> custom,
                                   String description) {
    return log(error, custom, description, Level.DEBUG);
  }

  /**
   * Log an error at the level returned by {@link Rollbar#level}.
   *
   * @param error the error.
   *
   * @return a {@link Publisher} which will execute the operation once a subscription requests it.
   */
  public Publisher<Response> log(Throwable error) {
    return log(error, null, null, null);
  }


  /**
   * Log an error at level specified.
   *
   * @param error the error.
   * @param level the level of the error.
   *
   * @return a {@link Publisher} which will execute the operation once a subscription requests it.
   */
  public Publisher<Response> log(Throwable error, Level level) {
    return log(error, null, null, level);
  }

  /**
   * Record an error or message with extra data at the level specified. At least one of `error` or
   * `description` must be non-null. If `error` is null, `description` will be sent as a message. If
   * `error` is non-null, `description` will be sent as the description of the error. Custom data
   * will be attached to message if the `error` is null. Custom data will extend whatever
   * {@link Config#custom} returns.
   *
   * @param error the error (if any).
   * @param custom the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level the level to send it at.
   *
   * @return a {@link Publisher} which will execute the operation once a subscription requests it.
   */
  public Publisher<Response> log(Throwable error, Map<String, Object> custom,
                                 String description, Level level) {
    return log(error, custom, description, level, false);
  }

  /**
   * Record an error or message with extra data at the level specified. At least one of `error` or
   * `description` must be non-null. If `error` is null, `description` will be sent as a message. If
   * `error` is non-null, description will be sent as the description of the error. Custom data will
   * be attached to message if the `error` is null. Custom data will extend whatever {@link
   * Config#custom} returns.
   *
   * @param error the error (if any).
   * @param custom the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level the level to send it at.
   * @param isUncaught whether or not this data comes from an uncaught exception.
   *
   * @return a {@link Publisher} which will execute the operation once a subscription requests it.
   */
  public Publisher<Response> log(Throwable error, Map<String, Object> custom,
                                 String description, Level level, boolean isUncaught) {
    return this.log(wrapThrowable(error), custom, description, level, isUncaught);
  }

  /**
   * Record an error or message with extra data at the level specified. At least one of `error` or
   * `description` must be non-null. If `error` is null, `description` will be sent as a message. If
   * `error` is non-null, `description` will be sent as the description of the error. Custom data
   * will be attached to message if the `error` is null. Custom data will extend whatever
   * {@link Config#custom} returns.
   *
   * @param error the error (if any).
   * @param custom the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level the level to send it at.
   * @param isUncaught whether or not this data comes from an uncaught exception.
   *
   * @return a {@link Publisher} which will execute the operation once a subscription requests it.
   */
  public Publisher<Response> log(ThrowableWrapper error, Map<String, Object> custom,
                               String description, Level level, boolean isUncaught) {
    try {
      return process(error, custom, description, level, isUncaught);
    } catch (Exception e) {
      LOGGER.error("Error while processing payload to send to Rollbar: {}", e);
      return Utils.empty();
    }
  }
}
