package com.rollbar.spring.boot.webmvc;

import com.rollbar.notifier.Rollbar;
import com.rollbar.spring.webmvc.RollbarExceptionResolver;
import com.rollbar.web.listener.RollbarRequestListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RollbarSpringBootExceptionResolver extends RollbarExceptionResolver {

  private Rollbar rollbar;

  @Autowired
  public RollbarSpringBootExceptionResolver(Rollbar rollbar) {
    super(rollbar);
  }

  /**
   * Registering a RollbarRequestListener to attribute HTTP requests to be available in Rollbar.com
   */
  @Bean
  public ServletListenerRegistrationBean<RollbarRequestListener> listenerRegistrationBean() {
    ServletListenerRegistrationBean<RollbarRequestListener> bean =
            new ServletListenerRegistrationBean<>();
    bean.setListener(new RollbarRequestListener());
    return bean;
  }

}