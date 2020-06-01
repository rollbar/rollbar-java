package com.rollbar.spring.webmvc;

import com.rollbar.notifier.Rollbar;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RollbarHandlerExceptionResolver implements HandlerExceptionResolver {

  private Rollbar rollbar;

  @Autowired
  public RollbarHandlerExceptionResolver(Rollbar rollbar) {
    this.rollbar = rollbar;
  }

  @Override
  public ModelAndView resolveException(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler,
                                     Exception ex) {
    rollbar.error(ex);

    return null; // returning null forces other resolvers to handle the exception
  }

}