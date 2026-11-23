package com.rollbar.notifier.sender;

import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.util.ObjectsUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JVM shutdown hook that flushes the payloads still buffered in a {@link Sender sender}.
 *
 * <p>
 * A {@link BufferedSender} is flushed without being closed, and keeps sending afterwards. The JVM
 * runs shutdown hooks concurrently, so other hooks, such as a web server draining in-flight
 * requests, may go on logging errors long after this one finishes. Other senders are closed with
 * {@link Sender#close(boolean) close(true)}.
 * </p>
 *
 * <p>
 * The hook is bounded by a timeout. Flushing performs blocking network I/O, and the sender does
 * not impose a socket timeout of its own, so an unbounded flush would be able to stall JVM
 * shutdown for as long as the remote end kept the connection open. Instead the flush runs on a
 * separate daemon thread and this hook waits for it for at most {@code timeoutMillis}; once that
 * elapses shutdown continues regardless, and the daemon thread is torn down with the JVM.
 * </p>
 *
 * <p>
 * Package-private so that it can rely on {@link BufferedSender} internals without making them
 * public. Created through {@link SenderShutdownHooks}.
 * </p>
 */
class SenderShutdownHook extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(SenderShutdownHook.class);

  private static final String HOOK_THREAD_NAME = "rollbar-shutdown-hook";

  private static final String FLUSH_THREAD_NAME = "rollbar-shutdown-flush";

  private final Provider<Sender> senderProvider;

  private final long timeoutMillis;

  /**
   * Constructor.
   *
   * @param senderProvider supplies the sender to flush, resolved when the hook runs rather than
   *                       when it is registered, so that a sender replaced in the meantime through
   *                       {@link com.rollbar.notifier.Rollbar#configure} is the one flushed. Not
   *                       null, though it may return null.
   * @param timeoutMillis the maximum time to spend flushing, in milliseconds. Values of zero or
   *                      less disable waiting: the flush is still started, but shutdown does not
   *                      block on it.
   */
  SenderShutdownHook(Provider<Sender> senderProvider, long timeoutMillis) {
    super(HOOK_THREAD_NAME);
    ObjectsUtils.requireNonNull(senderProvider, "The sender provider can not be null");
    this.senderProvider = senderProvider;
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public void run() {
    Thread flusher = new Thread(new FlushTask(), FLUSH_THREAD_NAME);
    // Daemon, so that failing to finish within the timeout can never keep the JVM alive.
    flusher.setDaemon(true);
    flusher.start();

    if (timeoutMillis <= 0) {
      return;
    }

    try {
      flusher.join(timeoutMillis);
    } catch (InterruptedException e) {
      // Preserve the interrupt for whatever else is running during shutdown.
      Thread.currentThread().interrupt();
      return;
    }

    if (flusher.isAlive()) {
      LOGGER.warn("Timed out after {} ms flushing Rollbar payloads on shutdown. Some occurrences "
          + "may not have been sent.", timeoutMillis);
    }
  }

  private class FlushTask implements Runnable {
    @Override
    public void run() {
      try {
        Sender sender = senderProvider.provide();
        if (sender == null) {
          return;
        }
        if (sender instanceof BufferedSender) {
          // Not close(true): shutdown hooks run concurrently, so the application may keep logging
          // after this returns, and a closed sender would leave those payloads queued forever.
          ((BufferedSender) sender).flushForShutdown();
        } else {
          sender.close(true);
        }
      } catch (Exception e) {
        LOGGER.error("Error flushing Rollbar payloads on shutdown.", e);
      } catch (Throwable e) {
        // The JVM is on its way down and this thread has no other handler. Swallowing here keeps
        // an error in the notifier from surfacing as a confusing failure during shutdown.
        LOGGER.error("Error flushing Rollbar payloads on shutdown.", e);
      }
    }
  }
}
