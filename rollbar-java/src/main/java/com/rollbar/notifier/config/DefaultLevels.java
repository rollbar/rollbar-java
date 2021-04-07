package com.rollbar.notifier.config;

import com.rollbar.api.payload.data.Level;

/**
 * Holds the default level configuration for ocurrence reporting of different types.
 */
public class DefaultLevels {
  private Level message;
  private Level error;
  private Level throwable;

  /**
   * Initialises all levels to their defaults.
   */
  public DefaultLevels() {
    this.message = Level.WARNING;
    this.error = Level.CRITICAL;
    this.throwable = Level.ERROR;
  }

  /**
   * Initialises all levels based on an existing config.
   *
   * @param config An existing configuration
   */
  public DefaultLevels(CommonConfig config) {
    this.message = config.defaultMessageLevel();
    this.error = config.defaultErrorLevel();
    this.throwable = config.defaultThrowableLevel();
  }

  public Level getMessage() {
    return message;
  }

  public void setMessage(Level level) {
    this.message = level;
  }

  public Level getError() {
    return error;
  }

  public void setError(Level level) {
    this.error = level;
  }

  public Level getThrowable() {
    return throwable;
  }

  public void setThrowable(Level level) {
    this.throwable = level;
  }
}
