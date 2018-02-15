package com.rollbar.api.payload.data;

import static java.util.Collections.unmodifiableMap;
import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the server object sent to Rollbar.
 */
public class Server implements JsonSerializable {

  private final String host;

  private final String root;

  private final String branch;

  private final String codeVersion;

  private final Map<String, Object> metadata;

  private Server(Builder builder) {
    this.host = builder.host;
    this.root = builder.root;
    this.branch = builder.branch;
    this.codeVersion = builder.codeVersion;
    this.metadata = builder.metadata != null ? unmodifiableMap(builder.metadata) : null;
  }

  /**
   * Getter.
   * @return the host.
   */
  public String getHost() {
    return host;
  }

  /**
   * Getter.
   * @return the root.
   */
  public String getRoot() {
    return root;
  }

  /**
   * Getter.
   * @return the branch.
   */
  public String getBranch() {
    return branch;
  }

  /**
   * Getter.
   * @return the code version.
   */
  public String getCodeVersion() {
    return codeVersion;
  }

  /**
   * Getter.
   * @return the metadata.
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public Map<String, Object> asJson() {
    Map<String, Object> values = new HashMap<>();

    if (metadata != null) {
      values.putAll(metadata);
    }
    if (host != null) {
      values.put("host", host);
    }
    if (root != null) {
      values.put("root", root);
    }
    if (branch != null) {
      values.put("branch", branch);
    }
    if (codeVersion != null) {
      values.put("code_version", codeVersion);
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

    Server server = (Server) o;

    if (host != null ? !host.equals(server.host) : server.host != null) {
      return false;
    }
    if (root != null ? !root.equals(server.root) : server.root != null) {
      return false;
    }
    if (branch != null ? !branch.equals(server.branch) : server.branch != null) {
      return false;
    }
    if (metadata != null ? !metadata.equals(server.metadata) : server.metadata != null) {
      return false;
    }
    return codeVersion != null ? codeVersion.equals(server.codeVersion)
        : server.codeVersion == null;
  }

  @Override
  public int hashCode() {
    int result = host != null ? host.hashCode() : 0;
    result = 31 * result + (root != null ? root.hashCode() : 0);
    result = 31 * result + (branch != null ? branch.hashCode() : 0);
    result = 31 * result + (codeVersion != null ? codeVersion.hashCode() : 0);
    result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Server{"
        + "host='" + host + '\''
        + ", root='" + root + '\''
        + ", branch='" + branch + '\''
        + ", codeVersion='" + codeVersion + '\''
        + ", metadata='" + metadata + '\''
        + '}';
  }

  /**
   * Builder class for {@link Notifier notifier}.
   */
  public static final class Builder {

    private String host;

    private String root;

    private String branch;

    private String codeVersion;

    private Map<String, Object> metadata;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param server the {@link Server server} to initialize a new builder instance.
     */
    public Builder(Server server) {
      this.host = server.host;
      this.root = server.root;
      this.branch = server.branch;
      this.codeVersion = server.codeVersion;
      this.metadata = server.metadata;
    }

    /**
     * The host.
     *
     * @param host the host.
     * @return the builder instance.
     */
    public Builder host(String host) {
      this.host = host;
      return this;
    }

    /**
     * The file system root.
     *
     * @param root the root.
     * @return the builder instance.
     */
    public Builder root(String root) {
      this.root = root;
      return this;
    }

    /**
     * The current source control branch.
     *
     * @param branch the branch.
     * @return the builder instance.
     */
    public Builder branch(String branch) {
      this.branch = branch;
      return this;
    }

    /**
     * The current source control version (SHA, or name).
     *
     * @param codeVersion the code version.
     * @return the builder instance.
     */
    public Builder codeVersion(String codeVersion) {
      this.codeVersion = codeVersion;
      return this;
    }

    /**
     * Extra metadata to include with the server.
     *
     * @param metadata the additional metadata.
     * @return the builder instance.
     */
    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    /**
     * Builds the {@link Server server}.
     *
     * @return the server.
     */
    public Server build() {
      return new Server(this);
    }
  }
}
