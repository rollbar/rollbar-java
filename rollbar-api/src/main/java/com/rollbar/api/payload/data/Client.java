package com.rollbar.api.payload.data;

import static com.rollbar.api.truncation.TruncationHelper.truncateStringsInMap;
import static com.rollbar.api.truncation.TruncationHelper.truncateStringsInNestedMap;
import static java.util.Collections.unmodifiableMap;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the client info to send to Rollbar.
 */
public class Client implements JsonSerializable, StringTruncatable<Client> {

  private static final long serialVersionUID = 1975664872679919021L;

  private final Map<String, Map<String, Object>> data;
  private final Map<String, Object> topLevelData;

  private Client(Builder builder) {
    this(builder.data, builder.topLevelData);
  }

  private Client(Map<String, Map<String, Object>> data, Map<String, Object> topLevelData) {
    this.data = unmodifiableMap(new HashMap<>(data));
    this.topLevelData = unmodifiableMap(new HashMap<>(topLevelData));
  }

  /**
   * Getter.
   * @return the data.
   */
  public Map<String, Map<String, Object>> getData() {
    return data;
  }

  /**
   * Getter.
   * @return the top level data.
   */
  public Map<String, Object> getTopLevelData() {
    return topLevelData;
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();
    values.putAll(data);
    values.putAll(topLevelData);
    return values;
  }


  @Override
  public Client truncateStrings(int maxLength) {
    // The Client builder class is different from the rest, it doesn't map to each of the built
    // object's properties, so we call the constructor directly.
    Map<String, Map<String, Object>> truncatedData = truncateStringsInNestedMap(data, maxLength);
    Map<String, Object> truncatedTopLevelData = truncateStringsInMap(topLevelData, maxLength);
    return new Client(truncatedData, truncatedTopLevelData);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Client client = (Client) o;

    if (data != null ? !data.equals(client.data) : client.data != null) {
      return false;
    }
    return topLevelData != null
        ? topLevelData.equals(client.topLevelData) : client.topLevelData == null;
  }

  @Override
  public int hashCode() {
    int result = data != null ? data.hashCode() : 0;
    result = 31 * result + (topLevelData != null ? topLevelData.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Client{"
        + "topLevelData='" + topLevelData + '\''
        + ", data='" + data + '\''
        + '}';
  }

  /**
   * Constructor.
   */
  public static final class Builder {

    private Map<String, Map<String, Object>> data;
    private Map<String, Object> topLevelData;

    /**
     * Constructor.
     */
    public Builder() {
      this.data = new HashMap<>();
      this.topLevelData = new HashMap<>();
    }

    /**
     * Constructor.
     *
     * @param client the {@link Client client} to initialize a new builder instance.
     */
    public Builder(Client client) {
      this.data = client.data;
      this.topLevelData = client.topLevelData;
    }

    /**
     * Add information about a client using a format of property and name.
     *
     * @param clientName the client name.
     * @param property the property.
     * @param value the value of the property for that client.
     * @return the builder instance.
     */
    public Builder addClient(String clientName, String property, Object value) {
      Map<String, Object> values = data.get(clientName);

      if (values == null) {
        values = new HashMap<>();
      }

      values.put(property, value);

      data.put(clientName, values);

      return this;
    }

    /**
     * Add information about a client..
     *
     * @param clientName the client name.
     * @param properties the properties of the client.
     * @return the builder instance
     */
    public Builder addClient(String clientName, Map<String, Object> properties) {
      Map<String, Object> values = data.get(clientName);

      if (values == null) {
        values = new HashMap<>();
      }

      values.putAll(properties);

      data.put(clientName, values);

      return this;
    }

    /**
     * Add top level client information.
     *
     * @param property the property.
     * @param value the value of the property.
     * @return the builder instance
     */
    public Builder addTopLevel(String property, Object value) {
      topLevelData.put(property, value);
      return this;
    }

    /**
     * Builds the {@link Client client}.
     *
     * @return the client.
     */
    public Client build() {
      return new Client(this);
    }
  }
}
