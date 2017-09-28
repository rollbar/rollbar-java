package com.rollbar.notifier.uncaughtexception;

import com.rollbar.notifier.Rollbar;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Rollbar uncaught exception handler.
 * This exception handler logs the {@link Throwable error} to Rollbar and delegates to the previous
 * exception handler the proper handling of the {@link Throwable error}.
 */
public class RollbarUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  private final Rollbar rollbar;

  private final UncaughtExceptionHandler delegate;

  /**
   * Constructor.
   * @param rollbar the rollbar notifier.
   * @param delegate the uncaught exception handler to delegate.
   */
  public RollbarUncaughtExceptionHandler(Rollbar rollbar, UncaughtExceptionHandler delegate) {
    this.rollbar = rollbar;
    this.delegate = delegate;
  }

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    rollbar.log(throwable);

    if (delegate != null) {
      delegate.uncaughtException(thread, throwable);
    }
  }
}
