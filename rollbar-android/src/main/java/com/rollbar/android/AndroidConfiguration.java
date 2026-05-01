package com.rollbar.android;

import com.rollbar.android.anr.AnrConfiguration;
import com.rollbar.api.payload.data.Level;

public class AndroidConfiguration {
  private final AnrConfiguration anrConfiguration;
  private final boolean mustCaptureNavigationEvents;
  private final boolean mustCaptureLogsAsTelemetry;
  private final Level minimumLogCaptureLevel;

  AndroidConfiguration(Builder builder) {
    anrConfiguration = builder.anrConfiguration;
    mustCaptureNavigationEvents = builder.mustCaptureNavigationEvents;
    mustCaptureLogsAsTelemetry = builder.mustCaptureLogsAsTelemetry;
    minimumLogCaptureLevel = builder.minimumLogCaptureLevel;
  }

  public AnrConfiguration getAnrConfiguration() {
    return anrConfiguration;
  }

  public boolean mustCaptureNavigationEvents() {
    return mustCaptureNavigationEvents;
  }

  public boolean mustCaptureLogsAsTelemetry() {
    return mustCaptureLogsAsTelemetry;
  }

  public Level getMinimumLogCaptureLevel() {
    return minimumLogCaptureLevel;
  }


  public static final class Builder {
    private AnrConfiguration anrConfiguration;
    private boolean mustCaptureNavigationEvents = true;
    private boolean mustCaptureLogsAsTelemetry = false;
    private Level minimumLogCaptureLevel = Level.WARNING;

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

    /**
     * Enable or disable automatic capture of Android log output as telemetry events.
     * When enabled, logs emitted via {@code android.util.Log} (and any other source written to
     * logcat from this app's UID, including third-party libraries) at or above the configured
     * minimum level are recorded as log telemetry events with
     * {@link com.rollbar.api.payload.data.Source#CLIENT}.
     * Default is disabled.
     * @param mustCaptureLogsAsTelemetry if automatic capture must be enabled or disabled.
     * @return the builder instance
     */
    public Builder captureLogsAsTelemetry(boolean mustCaptureLogsAsTelemetry) {
      this.mustCaptureLogsAsTelemetry = mustCaptureLogsAsTelemetry;
      return this;
    }

    /**
     * Minimum log level to capture as telemetry when {@link #captureLogsAsTelemetry(boolean)}
     * is enabled. Default is {@link Level#WARNING}.
     * @param minimumLogCaptureLevel the minimum level (inclusive) to capture.
     * @return the builder instance
     */
    public Builder minimumLogCaptureLevel(Level minimumLogCaptureLevel) {
      this.minimumLogCaptureLevel = minimumLogCaptureLevel;
      return this;
    }

    public AndroidConfiguration build() {
      return new AndroidConfiguration(this);
    }
  }
}
