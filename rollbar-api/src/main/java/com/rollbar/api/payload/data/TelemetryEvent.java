package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;
import com.rollbar.api.truncation.TruncationHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TelemetryEvent implements JsonSerializable, StringTruncatable<TelemetryEvent>  {

    private final TelemetryType type;
    private final Level level;
    private final Long timestamp;
    private final Map<String, String> body;
    private static final long serialVersionUID = 2843361810242481727L;
    private static final String SOURCE = "client";
    private static final String MESSAGE_KEY = "message";

    private TelemetryEvent(
      TelemetryType telemetryType,
      Level level,
      Map<String, String> body
    ) {
        timestamp = System.currentTimeMillis();
        type = telemetryType;
        this.level = level;
        this.body = body;
    }


    public static TelemetryEvent log(Level level, final String message) {
        Map<String, String> body = new HashMap<String, String>() {
            private static final long serialVersionUID = 3746979871039874692L;
            {
                put(MESSAGE_KEY, message);
            }
        };
        return new TelemetryEvent(TelemetryType.LOG, level, body);
    }

    @Override
    public Map<String, Object> asJson() {
        Map<String, Object> values = new HashMap<>();
        values.put("type", type.asJson());
        values.put("level", level.asJson());
        values.put("source", SOURCE);
        values.put("timestamp_ms", timestamp);
        values.put("body", body);
        return values;
    }

    @Override
    public TelemetryEvent truncateStrings(int maxLength) {
        String message = body.get(MESSAGE_KEY);
        if (message == null) return this;
        return log(level, TruncationHelper.truncateString(message, maxLength));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, level, body);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelemetryEvent that = (TelemetryEvent) o;
        return Objects.equals(type, that.type) && Objects.equals(level, that.level) && Objects.equals(timestamp, that.timestamp) && Objects.equals(body, that.body);
    }

    @Override
    public String toString() {
        return "TelemetryEvent{" +
                "type='" + type.asJson() + '\'' +
                ", level='" + level.asJson() + '\'' +
                ", source='" + SOURCE + '\'' +
                ", timestamp_ms=" + timestamp +
                ", body=" + body +
                '}';
    }
}
