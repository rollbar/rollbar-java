package com.rollbar.notifier.provider.timestamp;

import com.rollbar.notifier.provider.Provider;

/**
 * Provides the current timestamp from {@link System#currentTimeMillis}.
 */
public class TimestampProvider implements Provider<Long> {

  @Override
  public Long provide() {
    return System.currentTimeMillis();
  }
}
