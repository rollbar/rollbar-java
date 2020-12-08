package com.example.rollbar.struts2.action;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.inject.Inject;
import com.rollbar.notifier.Rollbar;
import com.rollbar.struts.RollbarFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Struts2 action with Spring CDI.
 */
public class HelloRollbarAction extends ActionSupport {

  private static final AtomicInteger counter = new AtomicInteger(1);

  private final Rollbar rollbar;

  @Inject
  public HelloRollbarAction(RollbarFactory rollbarFactory) {
    this.rollbar = rollbarFactory.build();
  }

  public String index() {
   rollbar.info("Executing index action in HelloRollbarAction");

   return SUCCESS;
  }

  public String hello() {
    int current = counter.getAndAdd(1);
    if (current % 2 == 0) {
      throw new RuntimeException("Fatal error at hello rollbar action. Number: " + current);
    }
    return SUCCESS;
  }

}
