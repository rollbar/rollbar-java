package com.rollbar.api.payload;

import static com.rollbar.api.truncation.TruncationHelper.truncateStringsInObject;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.truncation.StringTruncatable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the payload to send to Rollbar. A successfully constructed Payload matches Rollbar's
 * spec, and should be successful when serialized and POSTed to the correct endpoint.
 */
public class Payload implements JsonSerializable, StringTruncatable<Payload> {

  private static final long serialVersionUID = 3054041443735286159L;

  private final String accessToken;

  private final Data data;

  public final String json;

  private int sendAttemptCount;

  private Payload(Builder builder) {
    this.accessToken = builder.accessToken;
    this.data = builder.data;
    this.json = null;
    this.sendAttemptCount = 0;
  }

  /**
    * Constructor.
    *
    * @param json the JSON payload.
    */
  public Payload(String json) {
    this.accessToken = null;
    this.data = null;
    this.json = json;
    this.sendAttemptCount = 0;
  }

  /**
   * Getter.
   * @return the access token.
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * Getter.
   * @return the data.
   */
  public Data getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Payload data = (Payload) o;

    if (accessToken != null ? !accessToken.equals(data.accessToken) : data.accessToken != null) {
      return false;
    }
    return this.data != null ? this.data.equals(data.data) : data.data == null;
  }

  @Override
  public int hashCode() {
    int result = accessToken != null ? accessToken.hashCode() : 0;
    result = 31 * result + (data != null ? data.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Payload{"
        + "accessToken='" + accessToken + '\''
        + ", data=" + data
        + '}';
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();

    if (accessToken != null) {
      values.put("access_token", accessToken);
    }
    if (data != null) {
      values.put("data", data);
    }

    return values;
  }

  @Override
  public Payload truncateStrings(int maxLength) {
    if (this.data == null) {
      return this;
    }

    return new Payload.Builder(this)
        .data(truncateStringsInObject(data, maxLength))
        .build();
  }

  public int getSendAttemptCount() {
    return sendAttemptCount;
  }

  public void incrementSendAttemptCount() {
    ++this.sendAttemptCount;
  }

  /**
   * Builder class for {@link Payload}.
   */
  public static class Builder {

    private String accessToken;

    private Data data;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Contructor.
     *
     * @param payload the {@link Payload payload} to initialize a new builder instance.
     */
    public Builder(Payload payload) {
      this.accessToken = payload.accessToken;
      this.data = payload.data;
    }

    /**
     * An access token with scope "post_server_item" or "post_client_item".
     *
     * @param accessToken the access token.
     * @return the builder instance.
     */
    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    /**
     * The {@link Data data} to send.
     *
     * @param data the data
     * @return the builder instance.
     */
    public Builder data(Data data) {
      this.data = data;
      return this;
    }

    /**
     * Builds the {@link Payload payload}.
     *
     * @return the payload.
     */
    public Payload build() {
      return new Payload(this);
    }
  }
}
