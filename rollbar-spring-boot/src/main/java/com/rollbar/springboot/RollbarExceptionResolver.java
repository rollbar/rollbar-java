package com.rollbar.springboot;

import com.rollbar.notifier.Rollbar;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;


public class RollbarExceptionResolver implements HandlerExceptionResolver, Ordered {

    private Rollbar rollbar;

    public RollbarExceptionResolver(Rollbar rollbar) {
        this.rollbar = rollbar;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {

        rollbar.error(ex);

        // null = run other HandlerExceptionResolvers to actually handle the exception
        return null;
    }

    @Override
    public int getOrder() {
        // ensure this resolver runs first so that all exceptions are reported
        return Integer.MIN_VALUE;
    }
}