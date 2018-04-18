package com.rollbar.notifier.wrapper;

/**
 * Throwable wrapper to wrap a {@link Throwable thowable} or to represent a not available one.
 */
public interface ThrowableWrapper {

  /**
   * Get the {@link Throwable throwable} class name.
   *
   * @return the class name.
   */
  String getClassName();

  /**
   * Get the {@link Throwable throwable} message.
   *
   * @return the message.
   */
  String getMessage();

  /**
   * Get the {@link Throwable throwable} stack trace elements.
   *
   * @return the stack trace elements.
   */
  StackTraceElement[] getStackTrace();

  /**
   * Get the {@link ThrowableWrapper throwable wrapped} cause.
   *
   * @return the cause.
   */
  ThrowableWrapper getCause();

  /**
   * Get the wrapped {@link Throwable throwable}.
   *
   * @return the throwable.
   */
  Throwable getThrowable();
}
