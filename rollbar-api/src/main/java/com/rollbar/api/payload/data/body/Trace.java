package com.rollbar.api.payload.data.body;

import static java.util.Collections.unmodifiableList;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represent a Stack Trace to send to Rollbar.
 */
public class Trace implements BodyContent, JsonSerializable {

  private final List<Frame> frames;

  private final ExceptionInfo exception;

  private Trace(Builder builder) {
    this.frames = unmodifiableList(new ArrayList<>(builder.frames));
    this.exception = builder.exception;
  }

  /**
   * Getter.
   * @return the frames.
   */
  public List<Frame> getFrames() {
    return this.frames;
  }

  /**
   * Getter.
   * @return the exception info.
   */
  public ExceptionInfo getException() {
    return this.exception;
  }

  @Override
  public String getKeyName() {
    return "trace";
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();

    if (frames != null) {
      values.put("frames", frames);
    }
    if (exception != null) {
      values.put("exception", exception);
    }

    return values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Trace trace = (Trace) o;

    if (frames != null ? !frames.equals(trace.frames) : trace.frames != null) {
      return false;
    }
    return exception != null ? exception.equals(trace.exception) : trace.exception == null;
  }

  @Override
  public int hashCode() {
    int result = frames != null ? frames.hashCode() : 0;
    result = 31 * result + (exception != null ? exception.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Trace{"
        + "frames=" + frames
        + ", exception=" + exception
        + '}';
  }

  /**
   * Builder class for {@link Trace trace}.
   */
  public static final class Builder {

    private List<Frame> frames;

    private ExceptionInfo exception;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * The {@link Trace trace} to initialize a new builder instance.
     *
     * @param trace the trace.
     */
    public Builder(Trace trace) {
      this.frames = trace.frames;
      this.exception = trace.exception;
    }

    /**
     * The frames making up the exception.
     *
     * @param frames the frames.
     * @return the builder instance.
     */
    public Builder frames(List<Frame> frames) {
      this.frames = frames;
      return this;
    }

    /**
     * Info about the exception.
     *
     * @param exception the exception.
     * @return the builder instance.
     */
    public Builder exception(ExceptionInfo exception) {
      this.exception = exception;
      return this;
    }

    /**
     * Builds the {@link Trace trace}.
     *
     * @return the trace.
     */
    public Trace build() {
      return new Trace(this);
    }
  }
}
