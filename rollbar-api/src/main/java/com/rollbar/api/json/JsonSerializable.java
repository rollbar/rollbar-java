package com.rollbar.api.json;

/**
 * An object that can be serialized to JSON.
 */
public interface JsonSerializable {

  /**
   * Returns the object that should be an equivalent representation of itself in json.
   *
   * @return the json equivalent representation.
   */
  Object asJson();
}
