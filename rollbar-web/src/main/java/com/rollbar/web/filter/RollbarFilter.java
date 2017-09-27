package com.rollbar.web.filter;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.web.provider.PersonProvider;
import com.rollbar.web.provider.RequestProvider;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class RollbarFilter implements Filter {

  private Rollbar rollbar;

  RollbarFilter(Rollbar rollbar) {
    this.rollbar = rollbar;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    String accessToken = filterConfig.getInitParameter("access_token");

    rollbar = Rollbar.init(withAccessToken(accessToken)
        .language("java")
        .request(new RequestProvider())
        .person(new PersonProvider())
        .build());
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
    } catch (Exception e) {
      sendToRollbar(e);
      throw e;
    }
  }

  private void sendToRollbar(Exception error) {
    try {
      rollbar.error(error);
    } catch (Exception e) {
      // Swallow it, it should be logged.
    }
  }

  @Override
  public void destroy() {
    rollbar = null;
  }
}
