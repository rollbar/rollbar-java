package com.rollbar.spring.boot.webmvc;

import com.rollbar.web.listener.RollbarRequestListener;

import jakarta.servlet.ServletContext;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

@Component
public class RollbarServletContextInitializer implements ServletContextInitializer {

  /**
   * Adds RollbarListener when app starts up. This enriches HTTP request data
   * with Rollbar exceptions. You can view this data from the Rollbar app.
   */
  @Override
  public void onStartup(ServletContext container) {

    // Attach the RollbarRequestListener to attribute HTTP Request data into the Exception object
    // This will be visible in Rollbar
    container.addListener(RollbarRequestListener.class);
  }

}
