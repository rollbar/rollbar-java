package com.rollbar.web.config;

import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;

public class FakeConfigProvider implements ConfigProvider {

  public static boolean CALLED = false;

  public FakeConfigProvider() {
    CALLED = true;
  }

  @Override
  public Config provide(ConfigBuilder builder) {
    return builder.build();
  }
}
