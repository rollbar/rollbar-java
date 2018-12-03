package com.rollbar.jvmti;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * CacheFrame is a frame generated from the native interface to hold a method and a list of local
 * variables for later use.
 */
public final class CacheFrame {
  private Method method;
  private final LocalVariable[] locals;

  /**
   * Constructor with the method and list of local variables.
   */
  public CacheFrame(Method method, LocalVariable[] locals) {
    this.method = method;
    this.locals = Arrays.copyOf(locals, locals.length);
  }

  /**
   * Getter.
   *
   * @return the method that generated this frame.
   */
  public Method getMethod() {
    return method;
  }

  /**
   * Getter.
   *
   * @return the local variables for this frame.
   */
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
