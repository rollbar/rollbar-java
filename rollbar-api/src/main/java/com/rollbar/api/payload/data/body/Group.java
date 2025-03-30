package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;
import com.rollbar.api.truncation.TruncationHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Group implements JsonSerializable, StringTruncatable<Group> {
  private final List<RollbarThread> rollbarThreads;

  public Group(List<RollbarThread> threads) {
    rollbarThreads = threads;
  }

  @Override
  public Object asJson() {
    HashMap<String, Object> values = new HashMap<>();

    if (rollbarThreads != null) {
      values.put("threads", rollbarThreads);
    }

    return values;
  }

  @Override
  public Group truncateStrings(int maxLength) {
    return new Group(TruncationHelper.truncate(rollbarThreads, maxLength));
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Group)) return false;
    Group group = (Group) o;
    return Objects.equals(rollbarThreads, group.rollbarThreads);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rollbarThreads);
  }
}
