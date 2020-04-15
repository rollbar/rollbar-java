package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents *non-stacktrace* information about an exception, like class, description, and
 * message.
 */
public class ExceptionInfo implements JsonSerializable {

  private static final long serialVersionUID = -2271411217988417830L;

  private final String className;

  private final String message;

  private final String description;

  private ExceptionInfo(Builder builder) {
    this.className = builder.className;
    this.message = builder.message;
    this.description = builder.description;
  }

  /**
   * Getter.
   * @return the name of the exception class.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Getter.
   * @return the exception message.
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Getter.
   * @return a human readable description of the exception.
   */
  public String getDescription() {
    return this.description;
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();

    if (className != null) {
      values.put("class", className);
    }
    if (message != null) {
      values.put("message", message);
    }
    if (description != null) {
      values.put("description", description);
    }

    return values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ExceptionInfo that = (ExceptionInfo) o;

    if (className != null ? !className.equals(that.className) : that.className != null) {
      return false;
    }
    if (message != null ? !message.equals(that.message) : that.message != null) {
      return false;
    }
    return description != null ? description.equals(that.description) : that.description == null;
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (message != null ? message.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ExceptionInfo{"
        + "classname='" + className + '\''
        + ", message='" + message + '\''
        + ", description='" + description + '\''
        + '}';
  }

  /**
   * Builder class for {@link ExceptionInfo exception info}.
   */
  public static final class Builder {

    private String className;

    private String message;

    private String description;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     * @param exceptionInfo the {@link ExceptionInfo expception info} to initialize a new builder
     *                      instance.
     */
    public Builder(ExceptionInfo exceptionInfo) {
      this.className = exceptionInfo.className;
      this.message = exceptionInfo.message;
      this.description = exceptionInfo.description;
    }

    /**
     * The name of the exception class.
     * @param className the name of the class.
     * @return the builder instance.
     */
    public Builder className(String className) {
      this.className = className;
      return this;
    }

    /**
     * The exception message.
     *
     * @param message the message.
     * @return the builder instance.
     */
    public Builder message(String message) {
      this.message = message;
      return this;
    }

    /**
     * A human readable description of the exception.
     *
     * @param description the description.
     * @return the builder instance.
     */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * Builds the {@link ExceptionInfo exception info}.
     *
     * @return the exception info.
     */
    public ExceptionInfo build() {
      return new ExceptionInfo(this);
    }
  }
}
