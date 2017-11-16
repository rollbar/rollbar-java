package com.rollbar.notifier.util;

import com.rollbar.notifier.provider.Provider;

public class ConstantTimeProvider implements Provider<Long> {

  private static final long CURRENT_TIME = System.currentTimeMillis();


  @Override
  public Long provide() {
    return CURRENT_TIME;
  }
}
