package com.example.springbootwebmvc;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

}
