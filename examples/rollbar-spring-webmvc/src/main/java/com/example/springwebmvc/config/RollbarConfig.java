package com.example.springwebmvc.config;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.spring.webmvc.RollbarSpringConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@Configuration()
@EnableWebMvc
@ComponentScan({"com.example.springwebmvc","com.rollbar.spring.webmvc"})
public class RollbarConfig {

  private Config getRollbarConfigs(String accessToken) {

    // Reference ConfigBuilder.java for all the properties you can set for Rollbar
    return RollbarSpringConfigBuilder
            .initConfigBuilderWithAccessToken(accessToken)
            .environment("development")
            .framework("spring-webmvc")
            .build();
  }

  /**
   * Register a Rollbar bean to configure App with Rollbar.
   */
  @Bean(name = "rollbar")
  public Rollbar rollbar() {
    return Rollbar.init(getRollbarConfigs("<ACCESS TOKEN>"));
  }

}