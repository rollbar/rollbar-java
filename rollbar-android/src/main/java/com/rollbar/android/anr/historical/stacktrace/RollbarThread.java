package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RollbarThread implements JsonSerializable {
  private  Long id;
  private  String name;
  private  String state;
  private  Boolean crashed;
  private  Boolean current;
  private  Boolean main;
  private  StackTrace stacktrace;

  private  Map<String, LockReason> heldLocks;

  public StackTraceElement[] toStackTraceElement() {
    return stacktrace.getStackTraceElements();
  }

  public void setId(final  Long id) {
    this.id = id;
  }

  public  String getName() {
    return name;
  }

  public void setName(final  String name) {
    this.name = name;
  }

  public void setCrashed(final  Boolean crashed) {
    this.crashed = crashed;
  }

  public void setCurrent(final  Boolean current) {
    this.current = current;
  }

  public void setStacktrace(final StackTrace stacktrace) {
    this.stacktrace = stacktrace;
  }
  
  public Boolean isMain() {
    return main;
  }

  public void setMain(final  Boolean main) {
    this.main = main;
  }

  public void setState(final  String state) {
    this.state = state;
  }

  public  Map<String, LockReason> getHeldLocks() {
    return heldLocks;
  }

  public void setHeldLocks(final  Map<String, LockReason> heldLocks) {
    this.heldLocks = heldLocks;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();

    if (id != null) {
      values.put(JsonKeys.ID, id);
    }
    if (name != null) {
      values.put(JsonKeys.NAME, name);
    }
    if (state != null) {
      values.put(JsonKeys.STATE, state);
    }
    if (crashed != null) {
      values.put(JsonKeys.CRASHED, crashed);
    }
    if (current != null) {
      values.put(JsonKeys.CURRENT, current);
    }
    if (main != null) {
      values.put(JsonKeys.MAIN, main);
    }
    if (stacktrace != null) {
      values.put(JsonKeys.STACKTRACE, stacktrace);
    }
    if (heldLocks != null) {
      values.put(JsonKeys.HELD_LOCKS, heldLocks);
    }
    return values;
  }

  @Override
  public String toString() {
    return "RollbarThread{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", state='" + state + '\'' +
        ", crashed=" + crashed +
        ", current=" + current +
        ", main=" + main +
        ", stacktrace=" + Arrays.toString(toStackTraceElement()) +
        ", heldLocks=" + heldLocks +
        '}';
  }

  public static final class JsonKeys {
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String STATE = "state";
    public static final String CRASHED = "crashed";
    public static final String CURRENT = "current";
    public static final String MAIN = "main";
    public static final String STACKTRACE = "stacktrace";
    public static final String HELD_LOCKS = "held_locks";
  }
}
