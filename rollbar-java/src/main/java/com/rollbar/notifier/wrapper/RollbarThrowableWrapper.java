package com.rollbar.notifier.wrapper;

import com.rollbar.api.payload.data.body.RollbarThread;
import com.rollbar.notifier.util.BodyFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link ThrowableWrapper throwable wrapper}.
 */
public class RollbarThrowableWrapper implements ThrowableWrapper {

  private final String className;

  private final String message;

  private final StackTraceElement[] stackTraceElements;

  private final ThrowableWrapper cause;

  private final Throwable throwable;

  private final RollbarThread rollbarThread;

  private final Map<Thread, StackTraceElement[]> allStackTraces;

  private final List<RollbarThread> rollbarThreads;

  /**
   * Constructor.
   *
   * @param throwable the throwable.
   */
  public RollbarThrowableWrapper(Throwable throwable) {
    this(
      throwable.getClass().getName(),
      throwable.getMessage(),
      throwable.getStackTrace(),
      getInnerThrowableWrapper(throwable),
      throwable,
      Thread.currentThread(),
      captureAllStackTraces(Thread.currentThread()),
        null
    );
  }

  /**
   * Constructor.
   *
   * @param throwable the throwable.
   */
  public RollbarThrowableWrapper(Throwable throwable, Thread thread) {
    this(
      throwable.getClass().getName(),
      throwable.getMessage(),
      throwable.getStackTrace(),
      getInnerThrowableWrapper(throwable),
      throwable,
      thread,
      captureAllStackTraces(thread),
        null
    );
  }

  /**
   * Constructor.
   *
   * @param throwable the throwable.
   */
  public RollbarThrowableWrapper(
      Throwable throwable,
      List<RollbarThread> rollbarThreads
  ) {
    this(
      throwable.getClass().getName(),
      throwable.getMessage(),
      throwable.getStackTrace(),
      getInnerThrowableWrapper(throwable),
      throwable,
      null,
      null,
      rollbarThreads
    );
  }

  /**
   * Private constructor used to not capture all stack traces in a trace chain
   * The unused parameter is used to have a method overload.
   *
   * @param throwable the throwable.
   * @param unusedParameter used to differentiate signatures.
   */
  private RollbarThrowableWrapper(Throwable throwable, boolean unusedParameter) {
    this(
        throwable.getClass().getName(),
        throwable.getMessage(),
        throwable.getStackTrace(),
        getInnerThrowableWrapper(throwable),
        throwable,
        null,
        null,
        null
    );
  }

  /**
   * Constructor.
   *
   * @param className          the class name.
   * @param message            the message.
   * @param stackTraceElements the stack trace elements.
   * @param cause              the cause.
   */
  public RollbarThrowableWrapper(
      String className,
      String message,
      StackTraceElement[] stackTraceElements,
      ThrowableWrapper cause
  ) {
    this(
        className,
        message,
        stackTraceElements,
        cause,
        null,
        null,
        null,
        null
    );
  }

  private RollbarThrowableWrapper(
      String className,
      String message,
      StackTraceElement[] stackTraceElements,
      ThrowableWrapper cause,
      Throwable throwable,
      Thread thread,
      Map<Thread, StackTraceElement[]> allStackTraces,
      List<RollbarThread> rollbarThreads
  ) {
    this.className = className;
    this.message = message;
    this.stackTraceElements = stackTraceElements;
    this.cause = cause;
    this.throwable = throwable;
    this.rollbarThread = new BodyFactory().from(thread);
    this.allStackTraces = allStackTraces;
    this.rollbarThreads = rollbarThreads;
  }

  private static ThrowableWrapper getInnerThrowableWrapper(Throwable throwable) {
    if (throwable.getCause() == null) {
      return null;
    }
    return new RollbarThrowableWrapper(throwable.getCause(), false);
  }

  private static Map<Thread, StackTraceElement[]> captureAllStackTraces(Thread thread) {
    if (thread == null) {
      return null;
    }

    return filter(thread, Thread.getAllStackTraces());
  }

  private static Map<Thread, StackTraceElement[]> filter(
      Thread thread, Map<Thread,
          StackTraceElement[]> allStackTraces
  ) {
    HashMap<Thread, StackTraceElement[]> filteredStackTraces = new HashMap<>();

    for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
      Thread entryThread = entry.getKey();

      if (!thread.equals(entryThread)) {
        filteredStackTraces.put(entryThread, entry.getValue());
      }
    }

    return filteredStackTraces;
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
  public RollbarThread getRollbarThread() {
    return rollbarThread;
  }

  @Override
  public Map<Thread, StackTraceElement[]> getAllStackTraces() {
    return allStackTraces;
  }

  @Override
  public List<RollbarThread> getRollbarThreads() {
    return rollbarThreads;
  }

  @Override
  public String toString() {
    return "RollbarThrowableWrapper{"
      + "className='" + className + '\''
      + ", message='" + message + '\''
      + ", stackTraceElements=" + Arrays.toString(stackTraceElements)
      + ", cause=" + cause
      + ", throwable=" + throwable
      + ", rollbarThread=" + rollbarThread
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

    if (className != null ? !className.equals(that.className) : that.className != null) {
      return false;
    }
    if (message != null ? !message.equals(that.message) : that.message != null) {
      return false;
    }
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    if (!Arrays.equals(stackTraceElements, that.stackTraceElements)) {
      return false;
    }
    if (cause != null ? !cause.equals(that.cause) : that.cause != null) {
      return false;
    }
    return throwable != null ? throwable.equals(that.throwable) : that.throwable == null;
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (message != null ? message.hashCode() : 0);
    result = 31 * result + Arrays.hashCode(stackTraceElements);
    result = 31 * result + (cause != null ? cause.hashCode() : 0);
    result = 31 * result + (throwable != null ? throwable.hashCode() : 0);
    return result;
  }
}
