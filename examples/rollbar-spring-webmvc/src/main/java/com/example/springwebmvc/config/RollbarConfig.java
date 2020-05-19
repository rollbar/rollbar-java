package com.example.springwebmvc.config;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.web.provider.RequestProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@Configuration()
@EnableWebMvc
@ComponentScan({"com.example.springwebmvc","com.rollbar.spring.webmvc"})
public class RollbarConfig {

  /**
   * Register a Rollbar bean to configure App with Rollbar.
   */
  @Bean(name = "rollbar")
  public Rollbar rollbar() {
    return Rollbar.init(withAccessToken("<ACCESS TOKEN>")
            .environment("development")
            .framework("spring-webmvc")
            .request(new RequestProvider.Builder().build())
            .build());
  }

}