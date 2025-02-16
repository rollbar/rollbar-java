package com.rollbar.android.anr.watchdog;

import static android.app.ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING;

import android.app.ActivityManager;
import android.content.Context;

import com.rollbar.android.anr.AnrException;
import com.rollbar.android.anr.AnrListener;
import com.rollbar.notifier.provider.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WatchDog extends Thread {
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
  private static final String MESSAGE = "Application Not Responding for at least %s ms.";

  private final LooperHandler uiHandler;
  private final Provider<Long> timeProvider;
  private volatile long lastKnownActiveUiTimestampMs = 0;
  private final AtomicBoolean reported = new AtomicBoolean(false);
  private final Runnable ticker;
  private final Context context;
  private final AnrListener anrListener;
  private final WatchdogConfiguration watchdogConfiguration;

  public WatchDog(
      Context context,
      AnrListener anrListener,
      LooperHandler looperHandler,
      WatchdogConfiguration watchdogConfiguration,
      Provider<Long> timeProvider
  ) {
    uiHandler = looperHandler;
    this.anrListener = anrListener;
    this.context = context;
    this.timeProvider = timeProvider;
    this.watchdogConfiguration = watchdogConfiguration;
    this.ticker =
        () -> {
          lastKnownActiveUiTimestampMs = timeProvider.provide();
          reported.set(false);
        };
  }

  @Override
  public void run() {
    ticker.run();

    while (!isInterrupted()) {
      uiHandler.post(ticker);

      try {
        Thread.sleep(watchdogConfiguration.getPollingIntervalMillis());
      } catch (InterruptedException e) {
        try {
          Thread.currentThread().interrupt();
        } catch (SecurityException ignored) {
          LOGGER.warn("Failed to interrupt due to SecurityException: {}", e.getMessage());
          return;
        }
        LOGGER.warn("Interrupted: {}", e.getMessage());
        return;
      }

      if (isMainThreadNotHandlerTicker()) {
        if (isProcessNotResponding() && reported.compareAndSet(false, true)) {
          anrListener.onAppNotResponding(makeException());
        }
      }
    }
  }

  private AnrException makeException() {
    return new AnrException(createAnrMessage(), uiHandler.getThread());
  }

  private String createAnrMessage() {
    return String.format(MESSAGE, watchdogConfiguration.getTimeOutMillis());
  }

  private boolean isMainThreadNotHandlerTicker() {
    long unresponsiveDurationMs = timeProvider.provide() - lastKnownActiveUiTimestampMs;
    return unresponsiveDurationMs > watchdogConfiguration.getTimeOutMillis();
  }

  private boolean isProcessNotResponding() {
    final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    if (activityManager == null) return true;

    List<ActivityManager.ProcessErrorStateInfo> processesInErrorState = null;
    try {
      processesInErrorState = activityManager.getProcessesInErrorState();
    } catch (Exception e) {
      LOGGER.error("Error getting ActivityManager#getProcessesInErrorState: {}", e.getMessage());
    }

    if (processesInErrorState == null) {
      return false;
    }

    for (ActivityManager.ProcessErrorStateInfo item : processesInErrorState) {
      if (item.condition == NOT_RESPONDING) {
        return true;
      }
    }

    return false;
  }
}
