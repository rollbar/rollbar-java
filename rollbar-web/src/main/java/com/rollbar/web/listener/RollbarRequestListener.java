package com.rollbar.web.listener;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

public class RollbarRequestListener implements ServletRequestListener {

  private static final ThreadLocal<HttpServletRequest> CURRENT_REQUEST = new ThreadLocal<>();

  public static HttpServletRequest getServletRequest() {
    return CURRENT_REQUEST.get();
  }

  @Override
  public void requestInitialized(ServletRequestEvent sre) {
    if (sre.getServletRequest() instanceof HttpServletRequest) {
      CURRENT_REQUEST.set((HttpServletRequest) sre.getServletRequest());
    }
  }

  @Override
  public void requestDestroyed(ServletRequestEvent sre) {
    CURRENT_REQUEST.remove();
  }
}
