package com.rollbar.web.filter;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.web.provider.PersonProvider;
import com.rollbar.web.provider.RequestProvider;
import java.io.IOException;
import java.lang.reflect.Constructor;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollbarFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RollbarFilter.class);

  static final String ACCESS_TOKEN_PARAM_NAME = "access_token";

  static final String USER_IP_HEADER_PARAM_NAME = "user_ip_header";

  static final String CONFIG_PROVIDER_CLASS_PARAM_NAME = "config_provider";

  private Rollbar rollbar;

  public RollbarFilter() {
    // Empty constructor.
  }

  RollbarFilter(Rollbar rollbar) {
    this.rollbar = rollbar;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    String accessToken = filterConfig.getInitParameter(ACCESS_TOKEN_PARAM_NAME);
    String userIpHeaderName = filterConfig.getInitParameter(USER_IP_HEADER_PARAM_NAME);
    String configProviderClassName =
        filterConfig.getInitParameter(CONFIG_PROVIDER_CLASS_PARAM_NAME);

    ConfigProvider configProvider = getConfigProvider(configProviderClassName);
    Config config;

    RequestProvider requestProvider = new RequestProvider.Builder()
        .userIpHeaderName(userIpHeaderName)
        .build();

    ConfigBuilder configBuilder = withAccessToken(accessToken)
        .request(requestProvider)
        .person(new PersonProvider());

    if (configProvider != null) {
      config = configProvider.provide(configBuilder);
    } else {
      config = configBuilder.build();
    }

    rollbar = Rollbar.init(config);
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
      LOGGER.error("Error sending to rollbar the error: ", error, e);
    }
  }

  @Override
  public void destroy() {
    rollbar = null;
  }

  private ConfigProvider getConfigProvider(String configProviderClassName) {
    ConfigProvider configProvider = null;

    if (configProviderClassName != null && !"".equals(configProviderClassName)) {
      Class userConfigProviderClass = null;
      try {
        userConfigProviderClass = Class.forName(configProviderClassName);
      } catch (Exception e) {
        LOGGER.error("Could not get the config provider class: {}.", configProviderClassName, e);
      }
      if (userConfigProviderClass != null) {
        try {
          Constructor<ConfigProvider> constructor = userConfigProviderClass.getConstructor();
          configProvider = constructor.newInstance();
        } catch (Exception e) {
          LOGGER.error("Could not create the config provider.", e);
        }
      }
    }
    return configProvider;
  }
}
