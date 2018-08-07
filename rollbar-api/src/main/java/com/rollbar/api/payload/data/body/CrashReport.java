package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a crash report (currently only for iOS, eventually Android, and maybe (if possible)
 * core and memory dumps).
 */
public class CrashReport implements BodyContent, JsonSerializable {

  private final String raw;

  private CrashReport(Builder builder) {
    this.raw = builder.raw;
  }

  /**
   * Getter.
   * @return the crash report string.
   */
  public String getRaw() {
    return this.raw;
  }

  @Override
  public String getKeyName() {
    return "crash_report";
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();

    if (raw != null) {
      values.put("raw", getRaw());
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

    CrashReport that = (CrashReport) o;

    return raw != null ? raw.equals(that.raw) : that.raw == null;
  }

  @Override
  public int hashCode() {
    return raw != null ? raw.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "CrashReport{"
        + "raw='" + raw + '\''
        + '}';
  }

  /**
   * Builder class for {@link CrashReport crash report}.
   */
  public static final class Builder {

    private String raw;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param crashReport the {@link CrashReport crash report} to initialize a new builder instance.
     */
    public Builder(CrashReport crashReport) {
      this.raw = crashReport.raw;
    }

    /**
     * The crash report string.
     *
     * @param raw the raw.
     * @return the builder instance.
     */
    public Builder raw(String raw) {
      this.raw = raw;
      return this;
    }

    /**
     * Builds the {@link CrashReport crash report}.
     *
     * @return the crash report.
     */
    public CrashReport build() {
      return new CrashReport(this);
    }
  }
}
