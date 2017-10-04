package com.rollbar.api.json;

import java.io.Serializable;

/**
 * An object that can be serialized to JSON.
 */
public interface JsonSerializable extends Serializable {

  /**
   * Returns the object that should be an equivalent representation of itself in json.
   *
   * @return the json equivalent representation.
   */
  Object asJson();
}
