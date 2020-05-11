package com.example.springwebmvc;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.spring.webmvc.RollbarExceptionResolver;
import com.rollbar.notifier.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

@SpringBootApplication
@Configuration()
@ComponentScan()
public class ExampleApplication {
  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

  /**
   * Handles all exceptions with Rollbar's Exception Resolver.
   */
  @Bean
  public HandlerExceptionResolver rollbarExceptionResolver() {

    // If you use a configBuilder the framework is set to spring-web-mvc
    return new RollbarExceptionResolver(withAccessToken("b2ca53e2976340d2bad66e8ca5581b03")
            .environment("development"));

    // If you want to override the configBuilder constructor you can pass a configured
    // Rollbar object
    //
    // return new RollbarExceptionResolver(
    //        Rollbar.init(withAccessToken("b2ca53e2976340d2bad66e8ca5581b03")
    //                .environment("development")
    //                .framework("some-framework")
    //                .build()));
  }

}
