package com.rollbar.notifier.config;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.sender.Sender;
import java.net.Proxy;

/**
 * The configuration for the {@link Rollbar notifier}.
 */
public interface Config extends CommonConfig {
  /**
   * Get the {@link Sender sender}.
   *
   * @return the sender.
   */
  Sender sender();

  /**
   * Get the {@link Proxy proxy}.
   *
   * @return the proxy.
   */
  Proxy proxy();
}
