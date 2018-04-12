package com.rollbar.notifier.result;

import com.rollbar.notifier.sender.result.Result;

import java.util.Arrays;

/**
 * Represents the Rollbar response.
 */
public class Response {

  private int status;

  private com.rollbar.notifier.sender.result.Result result;

  Response(Builder builder) {
    this.status = builder.status;
    this.result = builder.result;
  }

  /**
   * The status code of the response.
   * @return the status code.
   */
  public int getStatus() {
    return status;
  }

  /**
   * The Rollbar result from API.
   * @return the result.
   */
  public com.rollbar.notifier.sender.result.Result getResult() {
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Response response = (Response) o;
    return status == response.status
        && (result == response.result) || (result != null && result.equals(response.result));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {status, result});
  }

  @Override
  public String toString() {
    return "Response{"
        + "status=" + status
        + ", result=" + result
        + '}';
  }

  /**
   * Builder class for {@link Response}.
   */
  public static final class Builder {

    private int status;

    private com.rollbar.notifier.sender.result.Result result;

    /**
     * The status code of the response.
     * @param status the status.
     * @return the builder instance.
     */
    public Builder status(int status) {
      this.status = status;
      return this;
    }

    /**
     * The Rollbar result.
     * @param result the result.
     * @return the builder instance.
     */
    public Builder result(Result result) {
      this.result = result;
      return this;
    }

    /**
     * Builds the {@link Response response}.
     *
     * @return the response.
     */
    public Response build() {
      return new Response(this);
    }
  }
}
