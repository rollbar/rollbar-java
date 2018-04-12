package com.rollbar.notifier.sender.result;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the result returned by Rollbar.
 */
public class Result {

  private static final int ERROR_CODE = 1;

  private final int err;

  private final String content;

  private Result(Builder builder) {
    this.err = builder.err;
    this.content = builder.content;
  }

  /**
   * The err field of the json returned by Rollbar.
   * @return the err.
   */
  public int getErr() {
    return err;
  }

  /**
   * The content return by Rollbar, message/uuid.
   * @return the content.
   */
  public String getContent() {
    return content;
  }

  /**
   * Indicates if is an error.
   * @return true if error, otherwise false.
   */
  public boolean isError() {
    return err == ERROR_CODE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Result result = (Result) o;
    return err == result.err
            && content != null && content.equals(result.content);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {err, content});
  }

  @Override
  public String toString() {
    return "Result{"
        + "err=" + err
        + ", content='" + content + '\''
        + '}';
  }

  /**
   * Builder class for {@link Result}.
   */
  public static final class Builder {

    private int err;

    private String content;

    /**
     * The err returned by Rollbar.
     *
     * @param code the err.
     * @return the builder instance.
     */
    public Builder code(int code) {
      this.err = code;
      return this;
    }

    /**
     * The body of the response returned by Rollbar.
     *
     * @param content the content.
     * @return the builder instance.
     */
    public Builder body(String content) {
      this.content = content;
      return this;
    }

    /**
     * Builds the {@link Result result}.
     *
     * @return the result.
     */
    public Result build() {
      return new Result(this);
    }
  }
}
