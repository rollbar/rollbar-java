package com.example.springbootwebmvc;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.spring.webmvc.RollbarSpringConfigBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@Configuration()
@EnableWebMvc
@ComponentScan({
    "com.example.springbootwebmvc",
    "com.rollbar.spring",
})
public class RollbarConfig {

  @Value("${rollbar.access_token}")
  private String accessToken;

  @Value("${rollbar.environment}")
  private String environment;

  private Config getRollbarConfigs() {

    // Reference ConfigBuilder.java for all the properties you can set for Rollbar
    return RollbarSpringConfigBuilder.withAccessToken(this.accessToken)
            .environment(this.environment)
            .build();
  }

  /**
  * Register a Rollbar bean to configure App with Rollbar.
  */
  @Bean
  public Rollbar rollbar() {
    return new Rollbar(getRollbarConfigs());
  }

}