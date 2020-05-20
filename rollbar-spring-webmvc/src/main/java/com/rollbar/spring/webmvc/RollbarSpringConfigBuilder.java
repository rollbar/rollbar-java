package com.rollbar.spring.webmvc;

import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.provider.server.ServerProvider;
import com.rollbar.web.provider.RequestProvider;

public class RollbarSpringConfigBuilder extends ConfigBuilder {

  /**
   * Helper class to provide a Config Builder for Java Spring.
   */
  public RollbarSpringConfigBuilder(String accessToken) {
    super(accessToken);
    this.request = new RequestProvider.Builder().build();
    this.server = new ServerProvider();
    this.framework = "spring";
  }

}
