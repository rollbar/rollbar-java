package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * This represent a Group of trace chains in a Thread.
 * In Java we only send 1 TraceChain per Group, in other languages like Python (ExceptionGroup) or
 * JS (AggregatorError), it may be more TraceChains per group.
 */
public class Group implements JsonSerializable, StringTruncatable<Group> {
  private final TraceChain traceChain;

  public Group(TraceChain traceChain) {
    this.traceChain = traceChain;
  }

  /**
   * Getter.
   *
   * @return the trace chain.
   */
  public TraceChain getTraceChain() {
    return traceChain;
  }

  @Override
  public Object asJson() {
    HashMap<String, Object> values = new HashMap<>();
    values.put("trace_chain", traceChain);
    ArrayList<HashMap<String, Object>> traceChains = new ArrayList<>();
    traceChains.add(values);
    return traceChains;
  }

  @Override
  public Group truncateStrings(int maxLength) {
    return new Group(traceChain.truncateStrings(maxLength));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Group group = (Group) o;
    return Objects.equals(traceChain, group.traceChain);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(traceChain);
  }

  @Override
  public String toString() {
    return "Group{" +
        "traceChain=" + traceChain +
        '}';
  }
}
