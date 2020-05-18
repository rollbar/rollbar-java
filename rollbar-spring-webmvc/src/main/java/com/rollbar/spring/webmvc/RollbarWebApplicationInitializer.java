package com.rollbar.spring.webmvc;

import com.rollbar.web.filter.RollbarFilter;
import com.rollbar.web.listener.RollbarRequestListener;
import javax.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.WebApplicationInitializer;

public class RollbarWebApplicationInitializer implements WebApplicationInitializer {

  @Override
  @Autowired
  public void onStartup(ServletContext container) {

    // Attach the RollbarRequestListner to attribute HTTP Request data into the Exception object
    // This will be visible in Rollbar
    container.addListener(RollbarRequestListener.class);
    container.addFilter("RollbarFilter", new RollbarFilter())
            .addMappingForUrlPatterns(null, true, "/*");

  }

}