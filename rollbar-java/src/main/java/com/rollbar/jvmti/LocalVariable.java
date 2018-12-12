package com.rollbar.jvmti;

/**
 * LocalVariable is a data object reprsenting a live variable in a stack frame at the time of an
 * exception gathered by the native interface.
 */
public final class LocalVariable {
  private final String name;
  private final Object value;

  /**
   * Constructor with the variable name and value.
   */
  public LocalVariable(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Getter.
   *
   * @return the name of the variable.
   */
  public String getName() {
    return name;
  }

  /**
   * Getter.
   *
   * @return the value of the variable.
   */
  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "LocalVariable{"
      + "name='" + name + '\''
      + ", value=" + value
      + '}';
  }
}
