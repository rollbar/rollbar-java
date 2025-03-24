package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RollbarThread implements JsonSerializable, StringTruncatable<RollbarThread> {

  private final String name;
  private final String id;
  private final String priority;
  private final String state;
  private final BodyContent bodyContent;

  public RollbarThread(Thread thread, BodyContent bodyContent) {
    name = thread.getName();
    id = String.valueOf(thread.getId());
    priority = String.valueOf(thread.getPriority());
    state = thread.getState().toString();
    this.bodyContent = bodyContent;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();
    values.put("name", name);
    values.put("id", id);
    values.put("priority", priority);
    values.put("state", state);
    if (bodyContent != null) {
      values.put(bodyContent.getKeyName(), bodyContent);
    }
    return values;
  }

  @Override
  public RollbarThread truncateStrings(int maxLength) {
    return null;
  }

  @Override
  public String toString() {
    return "RollbarThread{" +
      "name='" + name + '\'' +
      ", id='" + id + '\'' +
      ", priority='" + priority + '\'' +
      ", state='" + state + '\'' +
      ", " + bodyContent.getKeyName() + "=" + bodyContent +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof RollbarThread)) return false;
    RollbarThread that = (RollbarThread) o;
    return Objects.equals(name, that.name) && Objects.equals(id, that.id) && Objects.equals(priority, that.priority) && Objects.equals(state, that.state) && Objects.equals(bodyContent, that.bodyContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, id, priority, state, bodyContent);
  }

  /**
   * Builder class for {@link RollbarThread RollbarThread}.
   */
  public static final class Builder {

  }
}
