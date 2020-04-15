package com.rollbar.api.payload.data;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Information about this notifier, or one based off of this.
 */
public class Notifier implements JsonSerializable {

  private static final long serialVersionUID = -2605608164795462842L;

  private final String name;

  private final String version;

  private Notifier(Builder builder) {
    this.name = builder.name;
    this.version = builder.version;
  }

  /**
   * Getter.
   * @return the name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Getter.
   * @return the version of the notifier.
   */
  public String getVersion() {
    return this.version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Notifier notifier = (Notifier) o;

    if (name != null ? !name.equals(notifier.name) : notifier.name != null) {
      return false;
    }
    return version != null ? version.equals(notifier.version) : notifier.version == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Notifier{"
        + "name='" + name + '\''
        + ", version='" + version + '\''
        + '}';
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();

    if (name != null) {
      values.put("name", name);
    }
    if (version != null) {
      values.put("version", version);
    }

    return values;
  }

  /**
   * Builder class for {@link Notifier notifier}.
   */
  public static final class Builder {

    private String name;

    private String version;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param notifier the {@link Notifier notifier} to initialize a new builder instance.
     */
    public Builder(Notifier notifier) {
      this.name = notifier.name;
      this.version = notifier.version;
    }

    /**
     * The name of the notifier.
     *
     * @param name the name.
     * @return the builder instance.
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * The version of the notifier.
     *
     * @param version the version.
     * @return the builder instance.
     */
    public Builder version(String version) {
      this.version = version;
      return this;
    }

    /**
     * Builds the {@link Notifier notifier}.
     *
     * @return the notifier.
     */
    public Notifier build() {
      return new Notifier(this);
    }
  }
}
