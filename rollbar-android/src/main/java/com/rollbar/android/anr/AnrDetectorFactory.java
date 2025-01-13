package com.rollbar.android.anr;

import android.content.Context;
import android.os.Build;

import com.rollbar.android.anr.historical.HistoricalAnrDetector;
import com.rollbar.android.anr.watchdog.WatchdogAnrDetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnrDetectorFactory {
  private final static Logger LOGGER = LoggerFactory.getLogger(AnrDetectorFactory.class);

  public static AnrDetector create(
      Context context,
      AnrListener anrListener
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      LOGGER.debug("Creating HistoricalAnrDetector");
      return new HistoricalAnrDetector(context, anrListener);
    } else {
      LOGGER.debug("Creating WatchdogAnrDetector");
      return new WatchdogAnrDetector(context, anrListener);
    }
  }
}
