package com.rollbar.android.anr;

import com.rollbar.android.anr.watchdog.WatchdogConfiguration;

public class AnrConfiguration {
  WatchdogConfiguration watchdogConfiguration;
  boolean captureHistoricalAnr;

  public AnrConfiguration(Builder builder) {
    this.watchdogConfiguration = builder.watchdogConfiguration;
    this.captureHistoricalAnr = builder.captureHistoricalAnr;
  }

  public static final class Builder {
    private boolean captureHistoricalAnr = true;
    private WatchdogConfiguration watchdogConfiguration = new WatchdogConfiguration.Builder().build();

    /**
     * The WatchdogConfiguration configuration, if this field is null, no ANR would be captured.
     * By default this feature is on, in build versions < 30.
     * @param watchdogConfiguration the Watchdog configuration
     * @return the builder instance
     */
    public Builder setWatchdogConfiguration(WatchdogConfiguration watchdogConfiguration) {
      this.watchdogConfiguration = watchdogConfiguration;
      return this;
    }

    /**
     * A flag to turn on or off the HistoricalAnr detector implementation.
     * This implementation is used if the build version is >= 30
     * @param captureHistoricalAnr HistoricalAnrDetector flag
     * @return the builder instance
     */
    public Builder setCaptureHistoricalAnr(boolean captureHistoricalAnr) {
      this.captureHistoricalAnr = captureHistoricalAnr;
      return this;
    }

    public AnrConfiguration build() {
      return new AnrConfiguration(this);
    }
  }
}
