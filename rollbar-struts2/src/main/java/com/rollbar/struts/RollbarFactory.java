package com.rollbar.struts;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.notifier.config.ConfigProviderHelper;
import com.rollbar.web.provider.PersonProvider;
import com.rollbar.web.provider.RequestProvider;

public class RollbarFactory {

  private String accessToken;

  private String userIpHeader;

  private String captureIp;

  private String environment;

  private String codeVersion;

  private String configProviderClassName;

  public RollbarFactory() {
    this(null, null, null, null, null, null);
  }

  /**
   * Constructor.
   * @param accessToken the rollbar access token.
   * @param userIpHeader request http header name used for getting user ip.
   * @param captureIp "true" or "false" values to capture and send user ip.
   * @param environment the environment.
   * @param codeVersion the code version.
   * @param configProviderClassName the class name used to use a custom configuration.
   */
  public RollbarFactory(String accessToken, String userIpHeader, String captureIp,
      String environment, String codeVersion, String configProviderClassName) {
    this.accessToken = accessToken;
    this.userIpHeader = userIpHeader;
    this.captureIp = captureIp;
    this.environment = environment;
    this.codeVersion = codeVersion;
    this.configProviderClassName = configProviderClassName;
  }

  /**
   * Build method to create a Rollbar object instance.
   * @return the rollbar instance.
   */
  public Rollbar build() {
    ConfigProvider configProvider = ConfigProviderHelper.getConfigProvider(configProviderClassName);
    Config config;

    RequestProvider requestProvider = new RequestProvider.Builder()
        .userIpHeaderName(userIpHeader)
        .captureIp(captureIp)
        .build();

    ConfigBuilder configBuilder = withAccessToken(accessToken)
        .request(requestProvider)
        .person(new PersonProvider())
        .environment(environment)
        .codeVersion(codeVersion);

    if (configProvider != null) {
      config = configProvider.provide(configBuilder);
    } else {
      config = configBuilder.build();
    }

    return new Rollbar(config);
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

}
