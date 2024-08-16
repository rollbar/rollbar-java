package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;

public enum Source implements JsonSerializable {
  CLIENT("client"),
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
