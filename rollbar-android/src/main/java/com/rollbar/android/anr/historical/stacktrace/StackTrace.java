package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackTrace implements JsonSerializable {
  private static final String FRAMES_KEY = "frames";
  private static final String SNAPSHOT_KEY = "snapshot";

  private final List<StackFrame> frames;
  private  Boolean snapshot;

  public StackTrace(final List<StackFrame> frames) {
    this.frames = frames;
  }

  public StackTraceElement[] getStackTraceElements() {
    StackTraceElement[] stackTraceElements = new StackTraceElement[frames.size()];
    for (int i = 0; i < frames.size(); i++) {
      stackTraceElements[i] = frames.get(i).toStackTraceElement();
    }
    return stackTraceElements;
  }

  public void setSnapshot(final Boolean snapshot) {
    this.snapshot = snapshot;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();

    if (frames != null) {
      values.put(FRAMES_KEY, frames);
    }
    if (snapshot != null) {
      values.put(SNAPSHOT_KEY, snapshot);
    }
    return values;
  }

}
