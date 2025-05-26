package com.rollbar.notifier.wrapper;

import com.rollbar.api.payload.data.body.RollbarThread;

import java.util.List;
import java.util.Map;

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

  /**
   * Get the RollbarThread {@link RollbarThread rollbarThread}.
   *
   * @return the rollbarThread.
   */
  RollbarThread getRollbarThread();

  /**
   * Get a map of stack traces for all live threads in the moment of the Exception.
   *
   * @return the map.
   */
  Map<Thread, StackTraceElement[]> getAllStackTraces();

  /**
   * Get a list of the RollbarThreads for this error.
   *
   * @return the RollbarThreads.
   */
  List<RollbarThread> getRollbarThreads();
}
