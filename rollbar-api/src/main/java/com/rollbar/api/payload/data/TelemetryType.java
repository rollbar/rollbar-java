package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;

/**
 * Represents the different types of {@link TelemetryEvent} available.
 */
public enum TelemetryType implements JsonSerializable {
  LOG("log"),
  MANUAL("manual"),
  NAVIGATION("navigation"),
  NETWORK("network");

  private final String jsonName;

  TelemetryType(String jsonName) {
    this.jsonName = jsonName;
  }

  @Override
  public Object asJson() {
    return jsonName;
  }
}
