package com.rollbar.web.example.config;

import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.web.example.server.ServerProvider;

public class MyConfigProvider implements ConfigProvider {

  @Override
  public Config provide(ConfigBuilder builder) {
    return builder.server(new ServerProvider()).build();
  }
}
