package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;

/**
 * The Source of a payload.
 */
public enum Source implements JsonSerializable {

  /**
   * A Client source (e.g. Android)
   */
  CLIENT("client"),

  /**
   * A Server source (e.g. Spring)
   */
  SERVER("server");

  private final String jsonName;

  Source(String jsonName) {
    this.jsonName = jsonName;
  }

  @Override
  public Object asJson() {
    return jsonName;
  }
}
