package com.example.springwebmvc;

import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
public class ExampleController {

  @Autowired
  private ApplicationContext context;


  /**
   * Example for calling rollbar.log.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String index() {
    Rollbar rollbar = (Rollbar)context.getBean(Rollbar.class);
    rollbar.log("log some error to Rollbar");
    return "index";
  }

  /**
   * Example of a caught exception and then re-raised for Rollbar to process it.
   */
  @RequestMapping(value = "/handledException", method = RequestMethod.GET)
  public String testHandledException() {
    try {
      int x = 1 / 0;
    } catch (Exception e) {
      Rollbar rollbar = (Rollbar)context.getBean(Rollbar.class);
      rollbar.log("caught an error");
      throw e; // you can re-raise to for the Rollbar error handler to process it;
    }
    return "index";
  }

  /**
   * Example of an unhandled exception and Rollbar still processing it.
   */
  @RequestMapping(value = "/exception", method = RequestMethod.GET)
  public void exception() {
    int x = 1 / 0;
  }

}
