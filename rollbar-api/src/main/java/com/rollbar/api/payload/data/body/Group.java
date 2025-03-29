package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.HashMap;
import java.util.List;

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
    return null;
  }
}
