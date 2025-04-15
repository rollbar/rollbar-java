package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;
import com.rollbar.api.truncation.TruncationHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an event that allows you to leave a 'breadcrumb' leading up to an exception.
 */
public class TelemetryEvent implements JsonSerializable, StringTruncatable<TelemetryEvent> {

  private final TelemetryType type;
  private final Level level;
  private final Long timestamp;
  private final Map<String, String> body;
  private final Source source;
  private static final long serialVersionUID = 2843361810242481727L;

  /**
   * Construct a TelemetryEvent.
   *
   * @param telemetryType {@link TelemetryType}
   * @param level         {@link Level}
   * @param timestamp     the timestamp for this TelemetryEvent
   * @param source        {@link Source}
   * @param body          a map containing all the data required by the {@link TelemetryType}
   */
  public TelemetryEvent(
      TelemetryType telemetryType,
      Level level,
      Long timestamp,
      Source source,
      Map<String, String> body
  ) {
    type = telemetryType;
    this.timestamp = timestamp;
    this.level = level;
    this.source = source;
    this.body = new HashMap<>(body);
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();
    values.put("type", type.asJson());
    values.put("level", level.asJson());
    values.put("source", source.asJson());
    values.put("timestamp_ms", timestamp);
    values.put("body", body);
    return values;
  }

  @Override
  public TelemetryEvent truncateStrings(int maxLength) {
    return new TelemetryEvent(
        this.type,
        this.level,
        this.timestamp,
        this.source,
        TruncationHelper.truncateStringsInStringMap(body, maxLength)
    );
  }

  @Override
  public String toString() {
    return "TelemetryEvent{"
        + "type='" + type.asJson() + '\''
        + ", level='" + level.asJson() + '\''
        + ", source='" + source + '\''
        + ", timestamp_ms=" + timestamp
        + ", body=" + body
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TelemetryEvent that = (TelemetryEvent) o;
    return type == that.type && level == that.level && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(body, that.body) && Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, level, timestamp, body, source);
  }
}
