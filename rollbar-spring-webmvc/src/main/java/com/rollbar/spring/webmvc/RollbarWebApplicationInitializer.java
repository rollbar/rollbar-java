package com.rollbar.spring.webmvc;

import javax.servlet.ServletContext;
import com.rollbar.web.listener.RollbarRequestListener;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.beans.factory.annotation.Autowired;

public class RollbarWebApplicationInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext container) {
        container.addListener(RollbarRequestListener.class);
    }

}