package com.rollbar.notifier.config;

import java.lang.reflect.Constructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static helper for the ConfigProvider class.
 */
public class ConfigProviderHelper {

  private static Logger thisLogger = LoggerFactory.getLogger("ConfigProviderHelper");

  /**
   * Instantiate a ConfigProvider object based on the class name given as parameter.
   * @param configProviderClassName the string name of the config provider class
   */
  public static ConfigProvider getConfigProvider(String configProviderClassName) {
    ConfigProvider configProvider = null;

    if (configProviderClassName != null && !"".equals(configProviderClassName)) {
      Class userConfigProviderClass = null;
      try {
        userConfigProviderClass = Class.forName(configProviderClassName);
      } catch (Exception e) {
        thisLogger.error("Could not get the config provider class: {}.",
                configProviderClassName, e.getMessage());
      }
      if (userConfigProviderClass != null) {
        try {
          Constructor<ConfigProvider> constructor = userConfigProviderClass.getConstructor();
          configProvider = constructor.newInstance();
        } catch (Exception e) {
          thisLogger.error("Could not create the config provider.", e);
        }
      }
    }
    return configProvider;
  }
}
