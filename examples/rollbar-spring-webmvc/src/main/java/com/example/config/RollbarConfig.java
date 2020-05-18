package com.example.springwebmvc;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.spring.webmvc.RollbarExceptionResolver;
import com.rollbar.web.provider.RequestProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration()
@ComponentScan({"com.example.springbootwebmvc","com.rollbar.spring.webmvc"})
public class RollbarConfig {

  @Value("${rollbar.access_token}")
  private String accessToken;

  @Value("${rollbar.environment}")
  private String environment;

  @Value("${rollbar.framework}")
  private String framework;

  /**
   * Register a Rollbar bean to configure App with Rollbar.
   */
  @Bean(name = "rollbar")
  public Rollbar rollbar() {
    return Rollbar.init(withAccessToken(accessToken)
                .environment(environment)
                .framework(framework)
                .request(new RequestProvider.Builder().build())
                .build());
  }

}