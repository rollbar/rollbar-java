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
    private static final String LOG_KEY_MESSAGE = "message";
    private static final String NAVIGATION_KEY_FROM = "from";
    private static final String NAVIGATION_KEY_TO = "to";
    private static final String NETWORK_KEY_METHOD = "method";
    private static final String NETWORK_KEY_URL = "url";
    private static final String NETWORK_KEY_STATUS_CODE = "status_code";

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

    private TelemetryEvent(
            Long timestamp,
            TelemetryType telemetryType,
            Level level,
            Map<String, String> body
    ) {
        this.timestamp = timestamp;
        type = telemetryType;
        this.level = level;
        this.body = body;
    }

    /**
     * Creates a Log TelemetryEvent ({@link TelemetryType#LOG}).
     *
     * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
     * @param message the message sent for this event (e.g. "hello world").
     */
    public static TelemetryEvent log(Level level, final String message) {
        Map<String, String> body = new HashMap<String, String>() {
            private static final long serialVersionUID = 3746979871039874692L;
            {
                put(LOG_KEY_MESSAGE, message);
            }
        };
        return new TelemetryEvent(TelemetryType.LOG, level, body);
    }

    /**
     * Creates a Manual TelemetryEvent ({@link TelemetryType#MANUAL}) .
     *
     * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
     * @param message the message sent for this event (e.g. "hello world").
     */
    public static TelemetryEvent manual(Level level, final String message) {
        Map<String, String> body = new HashMap<String, String>() {
            private static final long serialVersionUID = 3746979871039874692L;
            {
                put(LOG_KEY_MESSAGE, message);
            }
        };
        return new TelemetryEvent(TelemetryType.MANUAL, level, body);
    }

    /**
     * Creates a Navigation TelemetryEvent ({@link TelemetryType#NAVIGATION}) .
     *
     * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
     * @param from the starting point (e.g. "SettingView").
     * @param to the destination point (e.g. "HomeView").
     */
    public static TelemetryEvent navigation(Level level, final String from, final String to) {
        Map<String, String> body = new HashMap<String, String>() {
            private static final long serialVersionUID = 3746979871039874692L;
            {
                put(NAVIGATION_KEY_FROM, from);
                put(NAVIGATION_KEY_TO, to);
            }
        };
        return new TelemetryEvent(TelemetryType.NAVIGATION, level, body);
    }

    /**
     * Creates a Network TelemetryEvent ({@link TelemetryType#NETWORK}).
     *
     * @param level the TelemetryEvent severity (e.g. {@link Level#DEBUG}).
     * @param method the verb used (e.g. "POST").
     * @param url the api url (e.g. "<a href="http://rollbar.com/test/api">http://rollbar.com/test/api</a>").
     * @param statusCode the response status code (e.g. "404").
     */
    public static TelemetryEvent network(Level level, final String method, final String url, final String statusCode) {
        Map<String, String> body = new HashMap<String, String>() {
            private static final long serialVersionUID = 3746979871039874692L;
            {
                put(NETWORK_KEY_METHOD, method);
                put(NETWORK_KEY_URL, url);
                put(NETWORK_KEY_STATUS_CODE, statusCode);
            }
        };
        return new TelemetryEvent(TelemetryType.NETWORK, level, body);
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
        Map<String, String> truncatedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : body.entrySet()) {
            String truncatedValue = TruncationHelper.truncateString(entry.getValue(), maxLength);
            truncatedMap.put(entry.getKey(), truncatedValue);
        }
        return new TelemetryEvent(
                this.timestamp,
                this.type,
                this.level,
                truncatedMap
        );
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
