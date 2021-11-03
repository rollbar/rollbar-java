package com.rollbar.api.payload.data.body;

import com.rollbar.api.truncation.StringTruncatable;

/**
 * A marker interface for the contents of the rollbar body.
 */
public interface BodyContent extends StringTruncatable<BodyContent> {

  /**
   * The key name of the body content.
   *
   * @return the key name.
   */
  String getKeyName();
}
