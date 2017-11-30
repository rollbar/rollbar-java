package com.rollbar.api.payload.data.body;

/**
 * A marker interface for the contents of the rollbar body.
 */
public interface BodyContent {

  /**
   * The key name of the body content.
   *
   * @return the key name.
   */
  String getKeyName();
}
