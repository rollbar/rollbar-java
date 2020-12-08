package com.rollbar.struts.interceptor;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.rollbar.notifier.Rollbar;
import com.rollbar.struts.RollbarFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollbarExceptionInterceptor extends AbstractInterceptor {

  private static Logger LOGGER = LoggerFactory.getLogger(RollbarExceptionInterceptor.class);

  private String accessToken;

  private String userIpHeader;

  private String captureIp;

  private String environment;

  private String codeVersion;

  private String configProviderClassName;

  private Rollbar rollbar;

  public RollbarExceptionInterceptor() {
    this(null);
  }

  /**
   * Constructor used with CI.
   * @param rollbarFactory the rollbar factory.
   */
  @Inject
  public RollbarExceptionInterceptor(RollbarFactory rollbarFactory) {
    if (rollbarFactory != null) {
      this.rollbar = rollbarFactory.build();
    } else {
      this.rollbar = null;
    }
  }

  @Override
  public void init() {
    if (rollbar != null) {
      LOGGER.info("Rollbar instance found by Dependency Injection!. "
          + "Ignoring interceptor properties.");
      return;
    }

    LOGGER.debug("Rollbar instance not found by Dependency Injection!. "
        + "Using interceptor properties.");

    RollbarFactory rollbarFactory = new RollbarFactory(accessToken, userIpHeader, captureIp,
        environment, codeVersion, configProviderClassName);
    this.rollbar = rollbarFactory.build();
  }

  @Override
  public String intercept(ActionInvocation actionInvocation) throws Exception {
    try {
      return actionInvocation.invoke();
    } catch (Exception e) {
      rollbar.error(e);
      throw e;
    }
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void setUserIpHeader(String userIpHeader) {
    this.userIpHeader = userIpHeader;
  }

  public void setConfigProviderClassName(String configProviderClassName) {
    this.configProviderClassName = configProviderClassName;
  }

  public void setCaptureIp(String captureIp) {
    this.captureIp = captureIp;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public void setCodeVersion(String codeVersion) {
    this.codeVersion = codeVersion;
  }

  Rollbar getRollbar() {
    return this.rollbar;
  }
}
