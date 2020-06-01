package com.example.springwebmvc.controller;

import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
class IndexController {

  @Autowired
  private Rollbar rollbar;

  /**
   * Example for calling rollbar.log.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  @ResponseBody
  public String index() {
    System.out.println(rollbar);
    rollbar.log("log some error to Rollbar");
    return "index";
  }

  /**
   * Example of a caught exception and then re-raised for Rollbar to process it.
   */
  @RequestMapping(value = "/handledException", method = RequestMethod.GET)
  @ResponseBody
  public String testHandledException() {
    try {
      int x = 1 / 0;
    } catch (Exception e) {
      rollbar.log("caught an error");
      throw e; // you can re-raise to for the Rollbar error handler to process it;
    }
    return "index";
  }

  /**
   * Example of an unhandled exception and Rollbar still processing it.
  */
  @RequestMapping(value = "/exception", method = RequestMethod.GET)
  @ResponseBody
  public void exception() {
    int x = 1 / 0;
  }

}


