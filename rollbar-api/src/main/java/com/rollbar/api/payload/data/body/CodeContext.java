package com.rollbar.api.payload.data.body;

import static com.rollbar.api.truncation.TruncationHelper.truncateStringsInList;
import static java.util.Collections.unmodifiableList;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.truncation.StringTruncatable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the context around the code where the error occurred (lines before, 'pre', and after,
 * 'post').
 */
public class CodeContext implements JsonSerializable, StringTruncatable<CodeContext> {

  private static final long serialVersionUID = 1271972843983198079L;

  private final List<String> pre;

  private final List<String> post;

  private CodeContext(Builder builder) {
    this.pre = builder.pre != null ? unmodifiableList(new ArrayList<>(builder.pre)) : null;
    this.post = builder.post != null ? unmodifiableList(new ArrayList<>(builder.post)) : null;
  }

  /**
   * Getter.
   * @return the lines of code before the one that triggered the error.
   */
  public List<String> getPre() {
    return pre;
  }

  /**
   * Getter.
   * @return the lines of code after the one that triggered the error.
   */
  public List<String> getPost() {
    return post;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();

    if (pre != null) {
      values.put("pre", pre);
    }
    if (post != null) {
      values.put("post", post);
    }

    return values;
  }

  @Override
  public CodeContext truncateStrings(int maxLength) {
    return new CodeContext.Builder(this)
        .pre(truncateStringsInList(pre, maxLength))
        .post(truncateStringsInList(post, maxLength))
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

    CodeContext that = (CodeContext) o;

    if (pre != null ? !pre.equals(that.pre) : that.pre != null) {
      return false;
    }
    return post != null ? post.equals(that.post) : that.post == null;
  }

  @Override
  public int hashCode() {
    int result = pre != null ? pre.hashCode() : 0;
    result = 31 * result + (post != null ? post.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "CodeContext{"
        + "pre=" + pre
        + ", post=" + post
        + '}';
  }

  /**
   * Builder class for {@link CodeContext code context}.
   */
  public static final class Builder {

    private List<String> pre;

    private List<String> post;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     * @param codeContext the {@link CodeContext code context} to initialize a new builder instance.
     */
    public Builder(CodeContext codeContext) {
      this.pre = codeContext.pre;
      this.post = codeContext.post;
    }

    /**
     * The lines of code before the one that triggered the error.
     * @param pre the pre.
     * @return the builder instance.
     */
    public Builder pre(List<String> pre) {
      this.pre = pre;
      return this;
    }

    /**
     * The lines of code after the one that triggered the error.
     * @param post the post.
     * @return the builder instance.
     */
    public Builder post(List<String> post) {
      this.post = post;
      return this;
    }

    /**
     * Builds the {@link CodeContext code context}.
     * @return the code context.
     */
    public CodeContext build() {
      return new CodeContext(this);
    }
  }
}
