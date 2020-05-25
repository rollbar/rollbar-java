package com.rollbar.spring.boot.webmvc;

import com.rollbar.web.listener.RollbarRequestListener;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import javax.servlet.ServletContext;

@Configuration
public class RollbarSpringBootConfiguration {

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
