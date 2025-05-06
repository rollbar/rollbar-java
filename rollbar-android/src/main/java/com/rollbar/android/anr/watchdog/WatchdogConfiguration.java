package com.rollbar.android.anr.watchdog;

public class WatchdogConfiguration {
  private long pollingIntervalMillis;
  private long timeOutMillis;

  WatchdogConfiguration(Builder builder) {
    pollingIntervalMillis = builder.pollingIntervalMillis;
    timeOutMillis = builder.timeOutMillis;
  }

  /**
   * Returns the Watchdog pooling interval in millis.
   * Default is 500
   *
   * @return the pooling interval in millis
   */
  public long getPollingIntervalMillis() {
    return pollingIntervalMillis;
  }

  /**
   * Returns the ANR timeout in millis.
   * Default is 5000, 5 seconds
   *
   * @return the timeout in millis
   */
  public long getTimeOutMillis() {
    return timeOutMillis;
  }

  public static final class Builder {
    private long pollingIntervalMillis = 500;
    private long timeOutMillis = 5000;

    /**
     * Set the Watchdog pooling interval in millis.
     * Default is 500
     *
     * @param pollingIntervalMillis timeout in millis
     * @return the builder instance
     */
    public Builder setPollingIntervalMillis(long pollingIntervalMillis) {
      this.pollingIntervalMillis = pollingIntervalMillis;
      return this;
    }

    /**
     * Set the ANR timeout in millis.
     * Default is 5000, 5 seconds
     *
     * @param timeOutMillis timeout in millis
     * @return the builder instance
     */
    public Builder setTimeOutMillis(long timeOutMillis) {
      this.timeOutMillis = timeOutMillis;
      return this;
    }

    public WatchdogConfiguration build() {
      return new WatchdogConfiguration(this);
    }
  }
}
