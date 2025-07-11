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
      WatchdogConfiguration watchdogConfiguration,
      AnrListener anrListener
  ) {
    interruptWatchdog();
    createWatchdog(context, watchdogConfiguration, anrListener);
  }

  @Override
  public void init() {
    if (watchDog == null) return;

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
      WatchdogConfiguration watchdogConfiguration,
      AnrListener anrListener
  ) {
    if (context == null) return;
    if (anrListener == null) return;

    watchDog = new WatchDog(
        context,
        anrListener,
        new LooperHandler(),
        watchdogConfiguration,
        new TimestampProvider()
    );
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
