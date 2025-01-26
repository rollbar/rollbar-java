package com.rollbar.android.anr;

import com.rollbar.android.anr.historical.stacktrace.RollbarThread;

import java.util.ArrayList;
import java.util.List;

public final class AnrException extends RuntimeException {

  private List<RollbarThread> threads = new ArrayList<>();

  public AnrException(String message, Thread thread) {
    super(message);
    setStackTrace(thread.getStackTrace());
  }

  public AnrException(StackTraceElement[] mainStackTraceElements, List<RollbarThread> threads) {
    super("Application Not Responding");
    setStackTrace(mainStackTraceElements);
    this.threads = threads;
  }

  public List<RollbarThread> getThreads() {
    return threads;
  }

}
