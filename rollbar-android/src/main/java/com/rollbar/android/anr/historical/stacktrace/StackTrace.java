package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackTrace implements JsonSerializable {

  private  List<StackFrame> frames;
  private  Map<String, String> registers;
  private  Boolean snapshot;

  @SuppressWarnings("unused")
  private  Map<String, Object> unknown;

  public StackTrace() {}

  public StackTrace(final  List<StackFrame> frames) {
    this.frames = frames;
  }

  public StackTraceElement[] getStackTraceElements() {
    StackTraceElement[] stackTraceElements = new StackTraceElement[frames.size()];
    int element = 0;
    for (StackFrame frame : frames) {
      stackTraceElements[element] = frame.toStackTraceElement();
      element++;
    }
    return stackTraceElements;
  }

  public  List<StackFrame> getFrames() {
    return frames;
  }

  public void setFrames(final  List<StackFrame> frames) {
    this.frames = frames;
  }

  public  Map<String, String> getRegisters() {
    return registers;
  }

  public void setRegisters(final  Map<String, String> registers) {
    this.registers = registers;
  }

  public  Boolean getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(final  Boolean snapshot) {
    this.snapshot = snapshot;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();

    if (frames != null) {
      values.put(JsonKeys.FRAMES, frames);
    }
    if (registers != null) {
      values.put(JsonKeys.REGISTERS, registers);
    }
    if (snapshot != null) {
      values.put(JsonKeys.SNAPSHOT, snapshot);
    }
    if (unknown != null) {
      for (String key : unknown.keySet()) {
        Object value = unknown.get(key);
        values.put(key, value);
      }
    }
    return values;
  }

  public static final class JsonKeys {
    public static final String FRAMES = "frames";
    public static final String REGISTERS = "registers";
    public static final String SNAPSHOT = "snapshot";
  }
}
