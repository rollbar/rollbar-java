package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import java.util.Objects;

/**
 * Represents a message (text) sent to Rollbar, possible with additional information.
 */
public class Message implements BodyContent, JsonSerializable {

  private final String body;

  private Message(Builder builder) {
    this.body = builder.body;
  }

  /**
   * Getter.
   * @return the body.
   */
  public String getBody() {
    return body;
  }

  @Override
  public String getKeyName() {
    return "message";
  }

  @Override
  public Object asJson() {
    return this.body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Message message = (Message) o;
    return Objects.equals(body, message.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(body);
  }

  @Override
  public String toString() {
    return "Message{"
        + "body='" + body + '\''
        + '}';
  }

  /**
   * Builder class for {@link Message message}.
   */
  public static final class Builder {

    private String body;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param message the {@link Message message} to initialize a new builder instance.
     */
    public Builder(Message message) {
      this.body = message.body;
    }

    /**
     * A string to send to Rollbar.
     *
     * @param body the body.
     * @return the builder instance.
     */
    public Builder body(String body) {
      this.body = body;
      return this;
    }

    /**
     * Builds the {@link CrashReport crash report}.
     *
     * @return the message.
     */
    public Message build() {
      return new Message(this);
    }
  }
}
