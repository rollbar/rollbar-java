package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackTrace implements JsonSerializable {

  private final List<StackFrame> frames;
  private  Boolean snapshot;

  public StackTrace(final  List<StackFrame> frames) {
    this.frames = frames;
  }

  public StackTraceElement[] getStackTraceElements() {
    StackTraceElement[] stackTraceElements = new StackTraceElement[frames.size()];
    for (int i = 0; i < frames.size(); i++) {
      stackTraceElements[i] = frames.get(i).toStackTraceElement();
    }
    return stackTraceElements;
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
    if (snapshot != null) {
      values.put(JsonKeys.SNAPSHOT, snapshot);
    }
    return values;
  }

  public static final class JsonKeys {
    public static final String FRAMES = "frames";
    public static final String SNAPSHOT = "snapshot";
  }
}
