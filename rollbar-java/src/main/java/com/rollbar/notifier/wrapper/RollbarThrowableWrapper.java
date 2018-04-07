package com.rollbar.notifier.wrapper;

import java.util.Arrays;
import java.util.Objects;

/**
 * Implementation of the {@link ThrowableWrapper throwable wrapper}.
 */
public class RollbarThrowableWrapper implements ThrowableWrapper {

  private final String className;

  private final String message;

  private final StackTraceElement[] stackTraceElements;

  private final ThrowableWrapper cause;

  private final Throwable throwable;

  /**
   * Constructor.
   *
   * @param throwable the throwable.
   */
  public RollbarThrowableWrapper(Throwable throwable) {
    this(throwable.getClass().getName(), throwable.getMessage(), throwable.getStackTrace(),
        throwable.getCause() != null ? new RollbarThrowableWrapper(throwable.getCause()) : null,
        throwable);
  }

  /**
   * Constructor.
   *
   * @param className the class name.
   * @param message the message.
   * @param stackTraceElements the stack trace elements.
   * @param cause the cause.
   */
  public RollbarThrowableWrapper(String className, String message,
      StackTraceElement[] stackTraceElements,
      ThrowableWrapper cause) {
    this(className, message, stackTraceElements, cause, null);
  }

  private RollbarThrowableWrapper(String className, String message, StackTraceElement[] stackTraceElements,
      ThrowableWrapper cause, Throwable throwable) {
    this.className = className;
    this.message = message;
    this.stackTraceElements = stackTraceElements;
    this.cause = cause;
    this.throwable = throwable;
  }

  @Override
  public String getClassName() {
    return this.className;
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public StackTraceElement[] getStackTrace() {
    return Arrays.copyOf(stackTraceElements, stackTraceElements.length);
  }

  @Override
  public ThrowableWrapper getCause() {
    return this.cause;
  }

  @Override
  public Throwable getThrowable() {
    return this.throwable;
  }

  @Override
  public String toString() {
    return "RollbarThrowableWrapper{"
        + "className='" + className + '\''
        + ", message='" + message + '\''
        + ", stackTraceElements=" + Arrays.toString(stackTraceElements)
        + ", cause=" + cause
        + ", throwable=" + throwable
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RollbarThrowableWrapper that = (RollbarThrowableWrapper) o;
    return Objects.equals(className, that.className)
        && Objects.equals(message, that.message)
        && Arrays.equals(stackTraceElements, that.stackTraceElements)
        && Objects.equals(cause, that.cause)
        && Objects.equals(throwable, that.throwable);
  }

  @Override
  public int hashCode() {

    int result = Objects.hash(className, message, cause, throwable);
    result = 31 * result + Arrays.hashCode(stackTraceElements);
    return result;
  }
}
