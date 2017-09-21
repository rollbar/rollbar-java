package com.rollbar.api.payload.data;

import static java.util.Collections.unmodifiableMap;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the client info to send to Rollbar.
 */
public class Client implements JsonSerializable {

  private final Map<String, Map<String, String>> data;

  private Client(Builder builder) {
    this.data = unmodifiableMap(builder.data);
  }

  /**
   * Getter.
   * @return the data.
   */
  public Map<String, Map<String, String>> getData() {
    return data;
  }

  @Override
  public Object asJson() {
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

    Client client = (Client) o;

    return data != null ? data.equals(client.data) : client.data == null;
  }

  @Override
  public int hashCode() {
    return data != null ? data.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Client{"
        + "data=" + data
        + '}';
  }

  /**
   * Constructor.
   */
  public static final class Builder {

    private Map<String, Map<String, String>> data;

    /**
     * Constructor.
     */
    public Builder() {
      this.data = new HashMap<>();
    }

    /**
     * Constructor.
     *
     * @param client the {@link Client client} to initialize a new builder instance.
     */
    public Builder(Client client) {
      this.data = client.data;
    }

    /**
     * Add information about a client using a format of property and name.
     *
     * @param clientName the client name.
     * @param property the property.
     * @param value the value of the property for that client.
     * @return the builder instance.
     */
    public Builder addClient(String clientName, String property, String value) {
      Map<String, String> values = data.get(clientName);

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
    public Builder addClient(String clientName, Map<String, String> properties) {
      Map<String, String> values = data.get(clientName);

      if (values == null) {
        values = new HashMap<>();
      }

      values.putAll(properties);

      data.put(clientName, values);

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
