package com.rollbar.android;

import com.rollbar.android.anr.AnrConfiguration;

public class AndroidConfiguration {
  private final AnrConfiguration anrConfiguration;

  AndroidConfiguration(Builder builder) {
    anrConfiguration = builder.anrConfiguration;
  }

  public AnrConfiguration getAnrConfiguration() {
    return anrConfiguration;
  }


  public static final class Builder {
    private AnrConfiguration anrConfiguration;

    Builder() {
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

    public AndroidConfiguration build() {
      return new AndroidConfiguration(this);
    }
  }
}
