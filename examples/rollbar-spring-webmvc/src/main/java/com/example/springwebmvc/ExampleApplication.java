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

@SpringBootApplication
@Configuration()
@ComponentScan({"com.example.springwebmvc","com.rollbar.spring.webmvc"})
public class ExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

  @Bean
  public Rollbar rollbar() {
    return Rollbar.init(withAccessToken("1b7dec62e84341ff8361b91d2c94e5b4")
            .environment("development")
            .framework("some-framework")
            .request(new RequestProvider.Builder().build())
            .build());
  }

}
