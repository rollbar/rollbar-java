package com.example.springwebmvc.initializer;

import com.example.springwebmvc.config.RollbarConfig;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;




public class AppInitializer implements WebApplicationInitializer {

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
    ctx.register(RollbarConfig.class);
    ctx.setServletContext(servletContext);

    ServletRegistration.Dynamic servlet =
            servletContext.addServlet("dispatcher",
                    new DispatcherServlet(ctx));
    servlet.setLoadOnStartup(1);
    servlet.addMapping("/");

  }

}