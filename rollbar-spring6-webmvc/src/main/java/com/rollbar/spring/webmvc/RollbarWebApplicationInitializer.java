package com.rollbar.spring.webmvc;

import com.rollbar.web.listener.RollbarRequestListener;
import jakarta.servlet.ServletContext;
import org.springframework.web.WebApplicationInitializer;

public class RollbarWebApplicationInitializer implements WebApplicationInitializer {

  @Override
  public void onStartup(ServletContext container) {

    // Attach the RollbarRequestListner to attribute HTTP Request data into the Exception object
    // This will be visible in Rollbar
    container.addListener(RollbarRequestListener.class);
  }

}
