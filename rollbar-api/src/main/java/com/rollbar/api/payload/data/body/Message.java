package com.rollbar.api.payload.data.body;

import static java.util.Collections.unmodifiableMap;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.TruncationHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a message (text) sent to Rollbar, possible with additional information.
 */
public class Message implements BodyContent, JsonSerializable {

  private static final long serialVersionUID = 1729529829545820666L;

  private final String body;
  private final Map<String, Object> metadata;

  private Message(Builder builder) {
    this.body = builder.body;
    this.metadata = builder.metadata != null
        ? unmodifiableMap(new HashMap<>(builder.metadata)) : null;
  }

  /**
   * Getter.
   * @return the body.
   */
  public String getBody() {
    return body;
  }

  /**
   * Getter.
   * @return the metadata.
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public String getKeyName() {
    return "message";
  }

  @Override
  public Object asJson() {
    Map<String, Object> message = new HashMap<>();
    if (this.metadata != null) {
      message.putAll(this.metadata);
    }
    message.put("body", this.body);
    return message;
  }

  @Override
  public Message truncateStrings(int maxLength) {
    if (this.metadata == null && this.body == null) {
      return this;
    }

    return new Message.Builder(this)
        .metadata(TruncationHelper.truncateStringsInMap(this.metadata, maxLength))
        .body(TruncationHelper.truncateString(getBody(), maxLength))
        .build();
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

    if (body != null ? !body.equals(message.body) : message.body != null) {
      return false;
    }
    return metadata != null ? metadata.equals(message.metadata) : message.metadata == null;
  }

  @Override
  public int hashCode() {
    int result = body != null ? body.hashCode() : 0;
    result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Message{"
        + "body='" + body + '\''
        + ", metadata='" + metadata + '\''
        + '}';
  }

  /**
   * Builder class for {@link Message message}.
   */
  public static final class Builder {

    private String body;
    private Map<String, Object> metadata;

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
     * Extra metadata to include with the message.
     *
     * @param metadata the additional metadata.
     * @return the builder instance.
     */
    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
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
