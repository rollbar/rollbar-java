package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;

public enum TelemetryType implements JsonSerializable {
  LOG("log");

  private final String jsonName;

  TelemetryType(String jsonName) {
    this.jsonName = jsonName;
  }

  @Override
  public Object asJson() {
    return jsonName;
  }
}
