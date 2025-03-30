package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Group implements JsonSerializable, StringTruncatable<Group> {
  private final BodyContent traceChain;

  public Group(BodyContent traceChain) {
    this.traceChain = traceChain;
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
    if (o == null || getClass() != o.getClass()) return false;
    Group group = (Group) o;
    return Objects.equals(traceChain, group.traceChain);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(traceChain);
  }
}
