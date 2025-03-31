package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.truncation.StringTruncatable;
import com.rollbar.api.truncation.TruncationHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * A container for the actual error(s), message, or crash report that caused this error.
 */
public class Body implements JsonSerializable, StringTruncatable<Body> {

  private static final long serialVersionUID = -2783273957046705016L;

  private final BodyContent bodyContent;

  private final List<TelemetryEvent> telemetryEvents;

  private final List<RollbarThread> rollbarThreads;

  private Body(Builder builder) {
    this.bodyContent = builder.bodyContent;
    this.telemetryEvents = builder.telemetryEvents;
    this.rollbarThreads = builder.rollbarThreads;
  }

  /**
   * Getter.
   *
   * @return the contents.
   */
  public BodyContent getContents() {
    return bodyContent;
  }

  @Override
  public Object asJson() {
    HashMap<String, Object> values = new HashMap<>();

    if (bodyContent != null) {
      values.put(bodyContent.getKeyName(), bodyContent);
    }

    if (telemetryEvents != null) {
      values.put("telemetry", telemetryEvents);
    }

    if (rollbarThreads != null) {
      values.put("threads", rollbarThreads);
    }

    return values;
  }

  @Override
  public Body truncateStrings(int maxSize) {
    if (bodyContent != null) {
      return new Body.Builder(this)
        .bodyContent(bodyContent.truncateStrings(maxSize))
        .telemetryEvents(TruncationHelper.truncate(telemetryEvents, maxSize))
        .rollbarThreads(TruncationHelper.truncate(rollbarThreads, maxSize))
        .build();
    } else {
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Body)) {
      return false;
    }

    Body body = (Body) o;
    return Objects.equals(bodyContent, body.bodyContent)
      && Objects.equals(telemetryEvents, body.telemetryEvents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bodyContent, telemetryEvents);
  }

  @Override
  public String toString() {
    return "Body{"
      + "bodyContent=" + bodyContent
      + ", telemetry=" + telemetryEvents
      + ", threads=" + rollbarThreads
      + '}';
  }

  /**
   * Builder class for {@link Body body}.
   */
  public static final class Builder {

    private BodyContent bodyContent;

    private List<TelemetryEvent> telemetryEvents;

    private List<RollbarThread> rollbarThreads;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param body the {@link Body body} to initialize a new builder instance.
     */
    public Builder(Body body) {
      bodyContent = body.bodyContent;
      telemetryEvents = body.telemetryEvents;
      rollbarThreads = body.rollbarThreads;
    }

    /**
     * The contents of this body (either {@link Trace}, {@link TraceChain}, {@link Message}, or
     * {@link CrashReport}).
     *
     * @param bodyContent the body content;
     * @return the builder instance.
     */
    public Builder bodyContent(BodyContent bodyContent) {
      this.bodyContent = bodyContent;
      return this;
    }

    /**
     * The Telemetry events of this body.
     *
     * @param telemetryEvents the events captured until this payload;
     * @return the builder instance.
     */
    public Builder telemetryEvents(List<TelemetryEvent> telemetryEvents) {
      this.telemetryEvents = telemetryEvents;
      return this;
    }

    /**
     * Information from threads.
     *
     * @param rollbarThreads a list of threads;
     * @return the builder instance.
     */
    public Builder rollbarThreads(List<RollbarThread> rollbarThreads) {
      this.rollbarThreads = rollbarThreads;
      return this;
    }

    /**
     * Builds the {@link Body body}.
     *
     * @return the body.
     */
    public Body build() {
      return new Body(this);
    }
  }
}
