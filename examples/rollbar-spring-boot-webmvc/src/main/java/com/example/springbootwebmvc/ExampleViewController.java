package com.example.springbootwebmvc;

import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleViewController {

  @Autowired
  private ApplicationContext context;

  /**
   * Testing an uncaught exception - The register Rollbar bean will pick this up.
   */
  @RequestMapping("/")
  public void exceptionTest() {

    // This exception will be passed now via the exception resolver
    int x = 1 / 0;

  }

  /**
   * Testing a handled exception. Rollbar will pick up uncaught automatically offering you
   * the option to send a custom log.
   */
  @RequestMapping("/handledExceptionTest")
  public void handledExceptionTest() {
    try {

      int x = 1 / 0;

    } catch (Exception e) {

      Rollbar rollbar = (Rollbar)context.getBean(Rollbar.class);
      rollbar.log("log some error to Rollbar");

      // continue to raise it and Rollbar will send the full payload
      throw e;
    }
  }

  /**
   * This is an example of how to access the Rollbar object and send an error.
   */
  @RequestMapping("/rollbarTest")
  public void rollbarTest() {

    Rollbar rollbar = (Rollbar)context.getBean(Rollbar.class);
    rollbar.error("Error");

  }
}