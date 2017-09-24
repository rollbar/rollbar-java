package com.rollbar.notifier.sender;

import java.util.Objects;

/**
 * Represents the result of the send process to Rollbar.
 */
public class Result {

  private final ResultCode code;

  private final String body;

  private Result(Builder builder) {
    this.code = builder.code;
    this.body = builder.body;
  }

  public ResultCode getCode() {
    return code;
  }

  public String getBody() {
    return body;
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
    return code == result.code
        && Objects.equals(body, result.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, body);
  }

  @Override
  public String toString() {
    return "Result{"
        + "code=" + code
        + ", body='" + body + '\''
        + '}';
  }

  /**
   * Builder class for {@link Result}.
   */
  public static final class Builder {

    private ResultCode code;

    private String body;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Contructor.
     *
     * @param result the {@link Result result} to initialize a new builder instance.
     */
    public Builder(Result result) {
      this.code = result.code;
      this.body = result.body;
    }

    /**
     * The {@link ResultCode code} returned by Rollbar.
     *
     * @param code the code.
     * @return the builder instance.
     */
    public Builder code(ResultCode code) {
      this.code = code;
      return this;
    }

    /**
     * The body of the response returned by Rollbar.
     *
     * @param body the body.
     * @return the builder instance.
     */
    public Builder body(String body) {
      this.body = body;
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
