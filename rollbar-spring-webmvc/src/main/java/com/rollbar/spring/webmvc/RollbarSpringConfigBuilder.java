package com.rollbar.spring.webmvc;

import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.web.provider.RequestProvider;

public class RollbarSpringConfigBuilder {

  /**
   * Returns a ConfigBuilder object and sets RequestProvider which
   * attributes HTTP request data in exceptions. This can be seen
   * in Rollbar.
   */
  public static ConfigBuilder initConfigBuilderWithAccessToken(String accessToken) {
    return ConfigBuilder
            .withAccessToken(accessToken)
            .request(new RequestProvider.Builder().build());
  }
}
