package com.example.springwebmvc;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.spring.webmvc.RollbarExceptionResolver;
import com.rollbar.web.provider.RequestProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
@Configuration()
@ComponentScan({"com.example.springwebmvc","com.rollbar.spring.webmvc"})
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

  @Bean(name="rollbar")
  public Rollbar rollbar() {
    return Rollbar.init(withAccessToken(accessToken)
            .environment(environment)
            .framework(framework)
            .request(new RequestProvider.Builder().build())
            .build());
  }

}
