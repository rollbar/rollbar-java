package com.rollbar.android.anr;

public final class AnrException extends RuntimeException {

  public AnrException(String message, Thread thread) {
    super(message);
    setStackTrace(thread.getStackTrace());
  }

  public AnrException(StackTraceElement[] stackTraceElements) {
    super("Application Not Responding");
    setStackTrace(stackTraceElements);
  }

}
