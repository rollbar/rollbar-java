package com.rollbar.notifier.config;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;

import java.net.Proxy;

/**
 * The configuration for the {@link Rollbar notifier}.
 */
public interface Config extends CommonConfig {
  /**
   * Get the {@link Sender sender}.
   *
   * @return the sender.
   */
  Sender sender();

  /**
   * Get the {@link Proxy proxy}.
   *
   * @return the proxy.
   */
  Proxy proxy();

  /**
   * Time in milliseconds the default sender waits while connecting to the Rollbar API.
   * Ignored when a custom {@link #sender() sender} is configured.
   *
   * @return the connect timeout in milliseconds.
   */
  default int connectTimeoutMillis() {
    return SyncSender.DEFAULT_CONNECT_TIMEOUT_MILLIS;
  }

  /**
   * Time in milliseconds the default sender waits for data from the Rollbar API once connected.
   * Ignored when a custom {@link #sender() sender} is configured.
   *
   * @return the read timeout in milliseconds.
   */
  default int readTimeoutMillis() {
    return SyncSender.DEFAULT_READ_TIMEOUT_MILLIS;
  }
}
