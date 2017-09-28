package com.rollbar.notifier.provider;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;

/**
 * Provider interface used to retrieve different data by {@link Rollbar the notifier} through the
 * {@link Config config} supplied to it.
 */
public interface Provider<T> {

  /**
   * Provides the value.
   *
   * @return the value.
   */
  T provide();
}
