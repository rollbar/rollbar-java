package com.rollbar.spring.boot.webmvc;

import com.rollbar.web.listener.RollbarRequestListener;

import javax.servlet.ServletContext;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RollbarSpringBootConfiguration {

  /**
   * Configures RollbarListener when app starts up. This enriches HTTP request data
   * with Rollbar exceptions. You can view this data from the Rollbar app.
  */
  @Bean
  public ServletContextInitializer rollbarRequestListenerInitializer() {
    return new ServletContextInitializer() {

      @Override
      public void onStartup(ServletContext container) {
        // Attach the RollbarRequestListner to attribute HTTP Request data into the Exception object
        // This will be visible in Rollbar
        container.addListener(RollbarRequestListener.class);
      }
    };
  }

}
