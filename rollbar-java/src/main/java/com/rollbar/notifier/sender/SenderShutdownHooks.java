package com.rollbar.notifier.sender;

import com.rollbar.notifier.provider.Provider;

/**
 * Creates the JVM shutdown hook that flushes the payloads still buffered in a sender.
 *
 * <p>
 * Used by {@link com.rollbar.notifier.Rollbar} to install the hook enabled through
 * {@link com.rollbar.notifier.config.Config#flushOnShutdown()}. It only returns a plain
 * {@link Thread}, so that how the hook flushes each kind of sender stays internal to this package.
 * </p>
 */
public final class SenderShutdownHooks {

  private SenderShutdownHooks() {
  }

  /**
   * Creates a shutdown hook, ready to be passed to {@link Runtime#addShutdownHook(Thread)}.
   *
   * @param senderProvider supplies the sender to flush, resolved when the hook runs rather than
   *                       when it is created. Not null, though it may return null.
   * @param timeoutMillis the maximum time to spend flushing, in milliseconds. Values of zero or
   *                      less disable waiting: the flush is still started, but shutdown does not
   *                      block on it.
   * @return the shutdown hook.
   */
  public static Thread create(Provider<Sender> senderProvider, long timeoutMillis) {
    return new SenderShutdownHook(senderProvider, timeoutMillis);
  }
}
