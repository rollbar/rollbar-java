package com.rollbar.api.payload.data.body;

import static java.util.Collections.unmodifiableList;

import com.rollbar.api.json.JsonSerializable;
import java.util.List;

/**
 * Represents a chain of errors (typically from Exceptions with {@link Exception#getCause()}
 * returning some value).
 */
public class TraceChain implements BodyContent, JsonSerializable {

  private final List<Trace> traces;

  public TraceChain(Builder builder) {
    this.traces = builder.traces != null ? unmodifiableList(builder.traces) : null;
  }

  /**
   * Getter.
   * @return a copy of the trace array.
   */
  public List<Trace> getTraces() {
    return this.traces;
  }

  @Override
  public String getKeyName() {
    return "trace_chain";
  }

  @Override
  public Object asJson() {
    return traces;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TraceChain that = (TraceChain) o;

    return traces != null ? traces.equals(that.traces) : that.traces == null;
  }

  @Override
  public int hashCode() {
    return traces != null ? traces.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "TraceChain{"
        + "traces=" + traces
        + '}';
  }

  /**
   * Builder class for {@link TraceChain trace chain}.
   */
  public static final class Builder {

    private List<Trace> traces;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param traceChain the {@link TraceChain trace chain} to initialize a new builder instance.
     */
    public Builder(TraceChain traceChain) {
      this.traces = traceChain.traces;
    }

    /**
     * The traces making up this trace chain.
     *
     * @param traces the traces.
     * @return the builder instance.
     */
    public Builder traces(List<Trace> traces) {
      this.traces = traces;
      return this;
    }

    /**
     * Builds the {@link TraceChain trace chain}.
     *
     * @return the trace chain.
     */
    public TraceChain build() {
      return new TraceChain(this);
    }
  }
}
