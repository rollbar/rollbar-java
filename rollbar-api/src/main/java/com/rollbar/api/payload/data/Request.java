package com.rollbar.api.payload.data;

import static java.util.Collections.unmodifiableMap;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the HTTP request that triggered the error.
 */
public class Request implements JsonSerializable {

  private final String url;

  private final String method;

  private final Map<String, String> headers;

  private final Map<String, String> params;

  private final Map<String, List<String>> get;

  private final String queryString;

  private final Map<String, Object> post;

  private final String body;

  private final String userIp;

  private final Map<String, Object> metadata;

  private Request(Builder builder) {
    this.url = builder.url;
    this.method = builder.method;
    this.headers = builder.headers;
    this.params = builder.params;
    this.get = builder.get;
    this.queryString = builder.queryString;
    this.post = builder.post;
    this.body = builder.body;
    this.userIp = builder.userIp;
    this.metadata = builder.metadata != null
        ? unmodifiableMap(new HashMap<>(builder.metadata)) : null;
  }

  /**
   * Getter.
   * @return the url.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Getter.
   * @return the HTTP method.
   */
  public String getMethod() {
    return method;
  }

  /**
   * Getter.
   * @return the HTTP headers.
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Getter.
   * @return the routing parameters.
   */
  public Map<String, String> getParams() {
    return params;
  }

  /**
   * Getter.
   * @return the parsed query string parameters
   */
  public Map<String, List<String>> getGet() {
    return get;
  }

  /**
   * Getter.
   * @return the raw query string.
   */
  public String getQueryString() {
    return queryString;
  }

  /**
   * Getter.
   * @return the parsed POST parameters.
   */
  public Map<String, Object> getPost() {
    return post;
  }

  /**
   * Getter.
   * @return the raw POST body.
   */
  public String getBody() {
    return body;
  }

  /**
   * Getter.
   * @return the affected user's IP address.
   */
  public String getUserIp() {
    return userIp;
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
    if (url != null) {
      values.put("url", url);
    }
    if (headers != null) {
      values.put("headers", headers);
    }
    if (params != null) {
      values.put("params", params);
    }
    if (get != null) {
      values.put("get", processGetParams(get));
    }
    if (queryString != null) {
      values.put("query_string", queryString);
    }
    if (post != null) {
      values.put("post", post);
    }
    if (body != null) {
      values.put("body", body);
    }
    if (userIp != null) {
      values.put("user_ip", userIp);
    }

    return values;
  }

  private Map<String, Object> processGetParams(Map<String, List<String>> map) {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
      String k = entry.getKey();
      List<String> v = entry.getValue();
      if (v.size() == 1) {
        result.put(k, v.get(0));
      } else {
        result.put(k, v);
      }
    }
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

    Request request = (Request) o;

    if (url != null ? !url.equals(request.url) : request.url != null) {
      return false;
    }
    if (method != null ? !method.equals(request.method) : request.method != null) {
      return false;
    }
    if (headers != null ? !headers.equals(request.headers) : request.headers != null) {
      return false;
    }
    if (params != null ? !params.equals(request.params) : request.params != null) {
      return false;
    }
    if (get != null ? !get.equals(request.get) : request.get != null) {
      return false;
    }
    if (queryString != null ? !queryString.equals(request.queryString)
        : request.queryString != null) {
      return false;
    }
    if (post != null ? !post.equals(request.post) : request.post != null) {
      return false;
    }
    if (body != null ? !body.equals(request.body) : request.body != null) {
      return false;
    }
    if (metadata != null ? !metadata.equals(request.metadata) : request.metadata != null) {
      return false;
    }
    return userIp != null ? userIp.equals(request.userIp) : request.userIp == null;
  }

  @Override
  public int hashCode() {
    int result = url != null ? url.hashCode() : 0;
    result = 31 * result + (method != null ? method.hashCode() : 0);
    result = 31 * result + (headers != null ? headers.hashCode() : 0);
    result = 31 * result + (params != null ? params.hashCode() : 0);
    result = 31 * result + (get != null ? get.hashCode() : 0);
    result = 31 * result + (queryString != null ? queryString.hashCode() : 0);
    result = 31 * result + (post != null ? post.hashCode() : 0);
    result = 31 * result + (body != null ? body.hashCode() : 0);
    result = 31 * result + (userIp != null ? userIp.hashCode() : 0);
    result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Request{"
        + "url='" + url + '\''
        + ", method='" + method + '\''
        + ", headers=" + headers
        + ", params=" + params
        + ", get=" + get
        + ", queryString='" + queryString + '\''
        + ", post=" + post
        + ", body='" + body + '\''
        + ", userIp='" + userIp + '\''
        + ", metadata='" + metadata + '\''
        + '}';
  }

  /**
   * Builder class for {@link Request request}.
   */
  public static final class Builder {

    private String url;

    private String method;

    private Map<String, String> headers;

    private Map<String, String> params;

    private Map<String, List<String>> get;

    private String queryString;

    private Map<String, Object> post;

    private String body;

    private String userIp;

    private Map<String, Object> metadata;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param request the {@link Request request} to initialize a new builder instance.
     */
    public Builder(Request request) {
      this.url = request.url;
      this.method = request.method;
      this.headers = request.headers;
      this.params = request.params;
      this.get = request.get;
      this.queryString = request.queryString;
      this.post = request.post;
      this.body = request.body;
      this.userIp = request.userIp;
      this.metadata = request.metadata;
    }

    /**
     * The url of the request.
     *
     * @param url the url.
     * @return the builder instance.
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * The HTTP method.
     *
     * @param method the method.
     * @return the builder instance.
     */
    public Builder method(String method) {
      this.method = method;
      return this;
    }

    /**
     * The HTTP Headers.
     *
     * @param headers the headers.
     * @return the builder instance.
     */
    public Builder headers(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    /**
     * The routing parameters (typically derived from your routing module).
     *
     * @param params the params.
     * @return the builder instance.
     */
    public Builder params(Map<String, String> params) {
      this.params = params;
      return this;
    }

    /**
     * The parsed query string parameters.
     *
     * @param get the get.
     * @return the builder instance.
     */
    public Builder get(Map<String, List<String>> get) {
      this.get = get;
      return this;
    }

    /**
     * The raw query string.
     *
     * @param queryString the querystring.
     * @return the builder instance.
     */
    public Builder queryString(String queryString) {
      this.queryString = queryString;
      return this;
    }

    /**
     * POST parameters.
     *
     * @param post the post.
     * @return the builder instance.
     */
    public Builder post(Map<String, Object> post) {
      this.post = post;
      return this;
    }

    /**
     * The raw POST body.
     *
     * @param body the body.
     * @return the builder instance.
     */
    public Builder body(String body) {
      this.body = body;
      return this;
    }

    /**
     * The ip address of the affected user.
     *
     * @param userIp the userIp.
     * @return the builder instance.
     */
    public Builder userIp(String userIp) {
      this.userIp = userIp;
      return this;
    }

    /**
     * Extra metadata to include with the request.
     *
     * @param metadata the additional metadata.
     * @return the builder instance.
     */
    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    /**
     * Builds the {@link Request request}.
     *
     * @return the request.
     */
    public Request build() {
      return new Request(this);
    }
  }
}
