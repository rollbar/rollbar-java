package com.rollbar.api.payload.data.body;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single frame from a stack trace.
 */
public class Frame implements JsonSerializable {

  private final String filename;

  private final Integer lineNumber;

  private final Integer columnNumber;

  private final String method;

  private final String code;

  private final String className;

  private final CodeContext context;

  private final List<Object> args;

  private final Map<String, Object> keywordArgs;

  private Frame(Builder builder) {
    this.filename = builder.filename;
    this.lineNumber = builder.lineNumber;
    this.columnNumber = builder.columnNumber;
    this.method = builder.method;
    this.code = builder.code;
    this.className = builder.className;
    this.context = builder.context;
    this.args = builder.args != null ? unmodifiableList(builder.args) : null;
    this.keywordArgs = builder.keywordArgs != null ? unmodifiableMap(builder.keywordArgs) : null;
  }

  /**
   * Getter.
   * @return the filename.
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Getter.
   * @return the line number.
   */
  public Integer getLineNumber() {
    return lineNumber;
  }

  /**
   * Getter.
   * @return the column number.
   */
  public Integer getColumnNumber() {
    return columnNumber;
  }

  /**
   * Getter.
   * @return the method.
   */
  public String getMethod() {
    return method;
  }

  /**
   * Getter.
   * @return the code.
   */
  public String getCode() {
    return code;
  }

  /**
   * Getter.
   * @return the classname.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Getter.
   * @return the context.
   */
  public CodeContext getContext() {
    return context;
  }

  /**
   * Getter.
   * @return the args.
   */

  public List<Object> getArgs() {
    return args;
  }

  /**
   * Getter.
   * @return the keyword arguments.
   */
  public Map<String, Object> getKeywordArgs() {
    return keywordArgs;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();

    values.put("filename", filename != null ? filename : "[unknown]");

    if (lineNumber != null) {
      values.put("lineno", lineNumber);
    }
    if (columnNumber != null) {
      values.put("colno", columnNumber);
    }
    if (method != null) {
      values.put("method", method);
    }
    if (code != null) {
      values.put("code", code);
    }
    if (className != null) {
      values.put("class_name", className);
    }
    if (context != null) {
      values.put("context", context);
    }
    if (args != null) {
      values.put("args", args);
    }
    if (keywordArgs != null) {
      values.put("kwargs", keywordArgs);
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

    Frame frame = (Frame) o;

    if (filename != null ? !filename.equals(frame.filename) : frame.filename != null) {
      return false;
    }
    if (lineNumber != null ? !lineNumber.equals(frame.lineNumber) : frame.lineNumber != null) {
      return false;
    }
    if (columnNumber != null ? !columnNumber.equals(frame.columnNumber)
        : frame.columnNumber != null) {
      return false;
    }
    if (method != null ? !method.equals(frame.method) : frame.method != null) {
      return false;
    }
    if (code != null ? !code.equals(frame.code) : frame.code != null) {
      return false;
    }
    if (className != null ? !className.equals(frame.className) : frame.className != null) {
      return false;
    }
    if (context != null ? !context.equals(frame.context) : frame.context != null) {
      return false;
    }
    if (args != null ? !args.equals(frame.args) : frame.args != null) {
      return false;
    }
    return keywordArgs != null ? keywordArgs.equals(frame.keywordArgs) : frame.keywordArgs == null;
  }

  @Override
  public int hashCode() {
    int result = filename != null ? filename.hashCode() : 0;
    result = 31 * result + (lineNumber != null ? lineNumber.hashCode() : 0);
    result = 31 * result + (columnNumber != null ? columnNumber.hashCode() : 0);
    result = 31 * result + (method != null ? method.hashCode() : 0);
    result = 31 * result + (code != null ? code.hashCode() : 0);
    result = 31 * result + (className != null ? className.hashCode() : 0);
    result = 31 * result + (context != null ? context.hashCode() : 0);
    result = 31 * result + (args != null ? args.hashCode() : 0);
    result = 31 * result + (keywordArgs != null ? keywordArgs.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Frame{"
        + "filename='" + filename + '\''
        + ", lineNumber=" + lineNumber
        + ", columnNumber=" + columnNumber
        + ", method='" + method + '\''
        + ", code='" + code + '\''
        + ", className='" + className + '\''
        + ", context=" + context
        + ", args=" + args
        + ", keywordArgs=" + keywordArgs
        + '}';
  }

  /**
   * Builder class for {@link Frame frame}.
   */
  public static final class Builder {

    private String filename;

    private Integer lineNumber;

    private Integer columnNumber;

    private String method;

    private String code;

    private String className;

    private CodeContext context;

    private List<Object> args;

    private Map<String, Object> keywordArgs;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param frame the {@link Frame frame} to initialize a new builder instance.
     */
    public Builder(Frame frame) {
      this.filename = frame.filename;
      this.lineNumber = frame.lineNumber;
      this.columnNumber = frame.columnNumber;
      this.method = frame.method;
      this.code = frame.code;
      this.className = frame.className;
      this.context = frame.context;
      this.args = frame.args;
      this.keywordArgs = frame.keywordArgs;
    }

    /**
     * The name of the file in which the error occurred.
     *
     * @param filename the filename.
     * @return the builder instance.
     */
    public Builder filename(String filename) {
      this.filename = filename;
      return this;
    }

    /**
     * The line number on which the error occurred.
     *
     * @param lineNumber the line number.
     * @return the builder instance.
     */
    public Builder lineNumber(Integer lineNumber) {
      this.lineNumber = lineNumber;
      return this;
    }

    /**
     * The column number (if available in your language) on which the error occurred.
     *
     * @param columnNumber the column number.
     * @return the builder instance.
     */
    public Builder columnNumber(Integer columnNumber) {
      this.columnNumber = columnNumber;
      return this;
    }

    /**
     * The method in which the error occurred.
     *
     * @param method the method.
     * @return the builder instance.
     */
    public Builder method(String method) {
      this.method = method;
      return this;
    }

    /**
     * The line of code that triggered the error.
     *
     * @param code the code.
     * @return the builder instance.
     */
    public Builder code(String code) {
      this.code = code;
      return this;
    }

    /**
     * The name of the class in which the error occurred.
     *
     * @param className the class name.
     * @return the builder instance.
     */
    public Builder className(String className) {
      this.className = className;
      return this;
    }

    /**
     * Extra context around the line of code that triggered the error.
     *
     * @param context the context.
     * @return the builder instance.
     */
    public Builder context(CodeContext context) {
      this.context = context;
      return this;
    }

    /**
     * The arguments to the method from the stack frame (if available in your language).
     *
     * @param args the args.
     * @return the builder instance.
     */
    public Builder args(List<Object> args) {
      this.args = args;
      return this;
    }

    /**
     * The keyword arguments to the method from the stack frame (if available in your language).
     *
     * @param keywordArgs the keyword args.
     * @return the builder instance.
     */
    public Builder keywordArgs(Map<String, Object> keywordArgs) {
      this.keywordArgs = keywordArgs;
      return this;
    }

    /**
     * Builds the {@link Frame frame}.
     *
     * @return the frame.
     */
    public Frame build() {
      return new Frame(this);
    }
  }
}
