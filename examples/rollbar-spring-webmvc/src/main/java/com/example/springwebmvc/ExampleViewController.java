package com.example.springwebmvc;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@RestController
public class ExampleViewController {

  @Autowired
  private ApplicationContext context;

  @RequestMapping("/")
  public void exceptionTest() {
    // This exception will be passed now via the exception resolver
    int x = 1 / 0;
  }

  @RequestMapping("/handledExceptionTest")
  public void handledExceptionTest() {
    // catch and send the exception back using Rollbar
    try {
      int x = 1 / 0;
    } catch(Exception e) {
      Rollbar rollbar = (Rollbar)context.getBean(Rollbar.class);
      rollbar.log("log some error to Rollbar");

      // continue to raise it and Rollbar will send the full payload
      throw e;
    }
  }

  @RequestMapping("/rollbarTest")
  public void rollbarTest() {
    // an example of sending the error back using the Rollbar object
    Rollbar rollbar = (Rollbar)context.getBean(Rollbar.class);
    rollbar.error("Error");
  }
}