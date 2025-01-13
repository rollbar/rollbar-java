package com.rollbar.android.anr.watchdog;

import android.annotation.SuppressLint;
import android.content.Context;

import com.rollbar.android.anr.AnrDetector;
import com.rollbar.android.anr.AnrListener;
import com.rollbar.notifier.provider.timestamp.TimestampProvider;

import java.io.Closeable;
import java.io.IOException;

public class WatchdogAnrDetector implements AnrDetector, Closeable {
  private boolean isClosed = false;
  private final Object startLock = new Object();

  @SuppressLint("StaticFieldLeak")
  private static WatchDog watchDog;
  private static final Object watchDogLock = new Object();

  public WatchdogAnrDetector(
      Context context,
      AnrListener anrListener
  ) {
    interruptWatchdog();
    createWatchdog(context, anrListener);
  }

  @Override
  public void init() {
    Thread thread = new Thread("WatchdogAnrDetectorThread") {
      @Override
      public void run() {
        super.run();
        synchronized (startLock) {
          if (!isClosed) {
            watchDog.start();
          }
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  public void close() throws IOException {
    synchronized (startLock) {
      isClosed = true;
    }
    interruptWatchdog();
  }

  private void createWatchdog(
      Context context,
      AnrListener anrListener
  ) {
    watchDog = new WatchDog(context, anrListener, new TimestampProvider());
  }

  private void interruptWatchdog() {
    synchronized (watchDogLock) {
      if (watchDog != null) {
        watchDog.interrupt();
        watchDog = null;
      }
    }
  }

}
