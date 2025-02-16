package com.rollbar.android.anr.watchdog;

import android.os.Handler;
import android.os.Looper;

public class LooperHandler {
  private final Handler handler;
  LooperHandler() {
    this.handler = new Handler(Looper.getMainLooper());
  }

  public void post(Runnable runnable) {
    handler.post(runnable);
  }

  public Thread getThread() {
    return handler.getLooper().getThread();
  }
}
