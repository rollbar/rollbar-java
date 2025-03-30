package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RollbarThread implements JsonSerializable, StringTruncatable<RollbarThread> {
  private final Thread thread;
  private final BodyContent bodyContent;

  public RollbarThread(Thread thread, BodyContent bodyContent) {
    this.thread = thread;
    this.bodyContent = bodyContent;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();
    values.put("name", getThreadName());
    values.put("id", getThreadId());
    values.put("priority", getThreadPriority());
    values.put("state", getThreadState());
    if (bodyContent != null) {
      values.put(bodyContent.getKeyName(), bodyContent);
    }
    return values;
  }

  @Override
  public RollbarThread truncateStrings(int maxLength) {
    return new RollbarThread(thread, bodyContent.truncateStrings(maxLength));
  }

  @Override
  public String toString() {
    return "RollbarThread{" +
      "name='" + getThreadName() + '\'' +
      ", id='" + getThreadId() + '\'' +
      ", priority='" + getThreadPriority() + '\'' +
      ", state='" + getThreadState() + '\'' +
      ", " + bodyContent.getKeyName() + "=" + bodyContent +
      '}';
  }

  private String getThreadName() {
    return thread.getName();
  }

  private String getThreadId() {
    return String.valueOf(thread.getId());
  }

  private String getThreadPriority() {
    return  String.valueOf(thread.getPriority());
  }

  private String getThreadState() {
    return thread.getState().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof RollbarThread)) return false;
    RollbarThread that = (RollbarThread) o;
    return Objects.equals(thread, that.thread) && Objects.equals(bodyContent, that.bodyContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(thread, bodyContent);
  }
}
