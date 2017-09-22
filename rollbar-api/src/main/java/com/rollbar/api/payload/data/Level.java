package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;

/**
 * The Level of a Rollbar Report.
 */
public enum Level implements Comparable<Level>, JsonSerializable {
  /**
   * A critical error (must be fixed ASAP).
   */
  CRITICAL("critical", 50),

  /**
   * An error, possibly customer facing, that should be fixed.
   */
  ERROR("error", 40),

  /**
   * An issue that may or may not be problematic.
   */
  WARNING("warning", 30),

  /**
   * Information about your software's operation.
   */
  INFO("info", 20),

  /**
   * Information to help in debugging your software.
   */
  DEBUG("debug", 10);

  private final String jsonName;
  private final int level;

  Level(String jsonName, int level) {
    this.jsonName = jsonName;
    this.level = level;
  }

  /**
   * Get the numeric value. Higher value, higher priority. Can be used to filter by some minimum
   * Level.
   *
   * @return the numeric priority, higher number, higher priority
   */
  public int level() {
    return level;
  }

  /**
   * How to serialize this as Json.
   *
   * @return the lower case string of the level
   */
  public String asJson() {
    return jsonName;
  }
}
