package com.example.springbootwebmvc;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.web.provider.RequestProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@SpringBootApplication
@Configuration()
@ComponentScan({"com.example.springbootwebmvc","com.rollbar.spring.boot.webmvc"})
public class ExampleApplication {

  @Value("${rollbar.access_token}")
  private String accessToken;

  @Value("${rollbar.environment}")
  private String environment;

  @Value("${rollbar.framework}")
  private String framework;


  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

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
