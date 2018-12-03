package com.rollbar.jvmti;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CacheFrame {
  private Method method;
  private final LocalVariable[] locals;

  public CacheFrame(Method method, LocalVariable[] locals) {
    this.method = method;
    this.locals = locals;
  }

  public Method getMethod() {
    return method;
  }

  public Map<String, Object> getLocals() {
    if (locals == null || locals.length == 0) {
        return Collections.emptyMap();
    }

    Map<String, Object> localsMap = new HashMap<>();
    for (LocalVariable localVariable : locals) {
      if (localVariable != null) {
        localsMap.put(localVariable.getName(), localVariable.getValue());
      }
    }

    return localsMap;
  }

  @Override
  public String toString() {
      return "CacheFrame{"
          + ", locals=" + Arrays.toString(locals)
          + '}';
  }
}
