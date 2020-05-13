package com.rollbar.spring.webmvc;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.web.listener.RollbarRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RollbarExceptionResolver implements HandlerExceptionResolver {

  private Rollbar rollbar;

  @Autowired
  public RollbarExceptionResolver(Rollbar rollbar) {
    this.rollbar = rollbar;
  }

  public RollbarExceptionResolver(ConfigBuilder configBuilder) {
    this.rollbar = Rollbar.init(configBuilder.build());
  }

  @Override
  public ModelAndView resolveException(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler,
                                     Exception ex) {
    rollbar.error(ex);

    return null; // returning null forces other resolvers to handle the exception
  }

  @Bean
  public ServletListenerRegistrationBean<RollbarRequestListener> listenerRegistrationBean() {
    ServletListenerRegistrationBean<RollbarRequestListener> bean =
            new ServletListenerRegistrationBean<>();
    bean.setListener(new RollbarRequestListener());
    return bean;
  }

}