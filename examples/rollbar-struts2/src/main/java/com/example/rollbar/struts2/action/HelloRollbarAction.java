package com.example.rollbar.struts2.action;

import com.opensymphony.xwork2.ActionSupport;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Struts2 action that rises an error every even request number.
 */
public class HelloRollbarAction extends ActionSupport {

  private static final AtomicInteger counter = new AtomicInteger(1);

  public String execute() {
    int current = counter.getAndAdd(1);
    if (current % 2 == 0) {
      throw new RuntimeException("Fatal error at hello rollbar action. Number: " + current);
    }
      return SUCCESS;
  }


}
