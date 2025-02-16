package com.rollbar.android.anr;

import android.content.Context;
import android.os.Build;

import com.rollbar.android.anr.historical.HistoricalAnrDetector;
import com.rollbar.android.anr.watchdog.WatchdogAnrDetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnrDetectorFactory {

  public static AnrDetector create(
      Context context,
      Logger logger,
      AnrConfiguration anrConfiguration,
      AnrListener anrListener
  ) {
    if (anrConfiguration == null) {
      logger.warn("No ANR configuration");
      return null;
    }
    if (context == null) {
      logger.warn("No context");
      return null;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!anrConfiguration.captureHistoricalAnr) {
        logger.warn("Historical ANR capture is off");
        return null;
      }

      logger.debug("Creating HistoricalAnrDetector");
      return new HistoricalAnrDetector(context, anrListener, createHistoricalAnrDetectorLogger());
    } else {
      if (anrConfiguration.watchdogConfiguration == null) {
        logger.warn("No Watchdog configuration");
        return null;
      }

      logger.debug("Creating WatchdogAnrDetector");
      return new WatchdogAnrDetector(
          context,
          anrConfiguration.watchdogConfiguration,
          anrListener
      );
    }
  }

  private static Logger createHistoricalAnrDetectorLogger() {
    return LoggerFactory.getLogger(HistoricalAnrDetector.class);
  }
}
