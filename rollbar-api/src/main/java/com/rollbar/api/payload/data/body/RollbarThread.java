package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents Thread information.
 */
public class RollbarThread implements JsonSerializable, StringTruncatable<RollbarThread> {
  private final String name;
  private final String id;
  private final String priority;
  private final String state;
  private final Group group;

  /**
   * Constructor.
   * @param thread the Thread.
   * @param group the Group of trace chains.
   */
  public RollbarThread(Thread thread, Group group) {
    name = thread.getName();
    id = String.valueOf(thread.getId());
    priority = String.valueOf(thread.getPriority());
    state = thread.getState().toString();
    this.group = group;
  }

  private RollbarThread(
      String name,
      String id,
      String priority,
      String state,
      Group group
  ) {
    this.name = name;
    this.id = id;
    this.priority = priority;
    this.state = state;
    this.group = group;
  }

  /**
   * Getter.
   *
   * @return the group for this Thread.
   */
  public Group getGroup() {
    return group;
  }

  /**
   * Getter.
   *
   * @return the state of this Thread.
   */
  public String getState() {
    return state;
  }

  /**
   * Getter.
   *
   * @return the priority of this Thread.
   */
  public String getPriority() {
    return priority;
  }

  /**
   * Getter.
   *
   * @return the id of this Thread.
   */
  public String getId() {
    return id;
  }

  /**
   * Getter.
   *
   * @return the name of this Thread.
   */
  public String getName() {
    return name;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();
    values.put("name", name);
    values.put("id", id);
    values.put("priority", priority);
    values.put("state", state);
    values.put("group", group);
    return values;
  }

  @Override
  public RollbarThread truncateStrings(int maxLength) {
    return new RollbarThread(
        name,
        id,
        priority,
        state,
        group.truncateStrings(maxLength)
    );
  }

  @Override
  public String toString() {
    return "RollbarThread{"
        + "name='" + name + '\''
        + ", id='" + id + '\''
        + ", priority='" + priority + '\''
        + ", state='" + state + '\''
        + ", group='" + group
        + '}';
  }


  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RollbarThread that = (RollbarThread) o;
    return Objects.equals(name, that.name)
        && Objects.equals(id, that.id)
        && Objects.equals(priority, that.priority)
        && Objects.equals(state, that.state)
        && Objects.equals(group, that.group);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, id, priority, state, group);
  }

  /**
   * Builder class for {@link RollbarThread RollbarThread}.
   */
  public static final class Builder {
    private String name;
    private String id;
    private String priority;
    private String state;
    private Group group;

    public Builder(RollbarThread rollbarThread) {
      name = rollbarThread.name;
      id = rollbarThread.id;
      priority = rollbarThread.priority;
      state = rollbarThread.state;
      group = rollbarThread.group;
    }

    /**
     * The group for this thread.
     *
     * @param group an updated version of group;
     * @return the builder instance.
     */
    public Builder group(Group group) {
      this.group = group;
      return this;
    }

    /**
     * Builds the {@link RollbarThread RollbarThread}.
     *
     * @return the RollbarThread.
     */
    public RollbarThread build() {
      return new RollbarThread(
        name,
        id,
        priority,
        state,
        group
      );
    }
  }
}
