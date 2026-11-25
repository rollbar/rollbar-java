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
  
  /**
   * <p>
   * If set to true (the default), a JVM shutdown hook is registered that flushes any payloads
   * still buffered in the {@link Sender sender} before the process exits.
   * </p>
   * <p>
   * The default sender buffers payloads in memory and drains them on a background daemon thread
   * every few seconds, so without this hook any occurrence captured shortly before the JVM
   * terminates is discarded. That includes short-lived processes and, more importantly,
   * {@code SIGTERM} during a rolling deploy or container eviction.
   * </p>
   * <p>
   * Set to false if the application manages the notifier lifecycle itself, for example by
   * calling {@link Rollbar#close(boolean)} explicitly, or when payloads are already persisted by
   * a durable queue.
   * </p>
   *
   * @return true to flush buffered payloads on JVM shutdown, false otherwise.
   */
  default boolean flushOnShutdown() {
    return true;
  }

  /**
   * <p>
   * The maximum time, in milliseconds, that the shutdown hook installed by
   * {@link #flushOnShutdown()} will spend flushing buffered payloads before letting the JVM
   * continue to exit. Ignored when {@link #flushOnShutdown()} is false.
   * </p>
   * <p>
   * This is a hard bound and not a target: shutdown always proceeds once it elapses, even if
   * payloads remain unsent. It exists because a flush performs blocking network I/O, and an
   * unbounded one would be able to stall JVM shutdown indefinitely.
   * </p>
   *
   * @return the shutdown flush timeout in milliseconds.
   */
  default long shutdownTimeoutMillis() {
    return 2000L;
  }
}
