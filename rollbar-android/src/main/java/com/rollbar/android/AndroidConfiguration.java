package com.rollbar.android;

import com.rollbar.android.anr.AnrConfiguration;

public class AndroidConfiguration {
  private final AnrConfiguration anrConfiguration;
  private final boolean mustCaptureNavigationEvents;

  AndroidConfiguration(Builder builder) {
    anrConfiguration = builder.anrConfiguration;
    mustCaptureNavigationEvents = builder.mustCaptureNavigationEvents;
  }

  public AnrConfiguration getAnrConfiguration() {
    return anrConfiguration;
  }

  public boolean mustCaptureNavigationEvents() {
    return mustCaptureNavigationEvents;
  }


  public static final class Builder {
    private AnrConfiguration anrConfiguration;
    private boolean mustCaptureNavigationEvents = true;

    public Builder() {
      anrConfiguration = new AnrConfiguration.Builder().build();
    }

    /**
     * The ANR configuration, if this field is null, no ANR would be captured
     * @param anrConfiguration the ANR configuration
     * @return the builder instance
     */
    public Builder setAnrConfiguration(AnrConfiguration anrConfiguration) {
      this.anrConfiguration = anrConfiguration;
      return this;
    }

    /**
     * Enable or disable automatic capture of Telemetry Navigation events (only over new Activities).
     * Default is enabled.
     * @param mustCaptureNavigationEvents if automatic capture must be enabled or disabled.
     * @return the builder instance
     */
    public Builder captureNewActivityTelemetryEvents(boolean mustCaptureNavigationEvents) {
      this.mustCaptureNavigationEvents = mustCaptureNavigationEvents;
      return this;
    }

    public AndroidConfiguration build() {
      return new AndroidConfiguration(this);
    }
  }
}
