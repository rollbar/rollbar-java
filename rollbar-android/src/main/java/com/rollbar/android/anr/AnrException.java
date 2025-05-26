package com.rollbar.android.anr;

import com.rollbar.api.payload.data.body.RollbarThread;

import java.util.List;

public final class AnrException extends RuntimeException {

  private List<RollbarThread> threads;

  public AnrException(String message, Thread thread) {
    super(message);
    setStackTrace(thread.getStackTrace());
  }

  public AnrException(List<RollbarThread> threads) {
    super("Application Not Responding");
    this.threads = threads;
  }

  public List<RollbarThread> getThreads() {
    return threads;
  }

}
