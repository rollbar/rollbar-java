package com.rollbar.api.payload.data.body;

import com.rollbar.api.json.JsonSerializable;
import java.util.HashMap;

/**
 * A container for the actual error(s), message, or crash report that caused this error.
 */
public class Body implements JsonSerializable {

  private static final long serialVersionUID = -2783273957046705016L;

  private final BodyContent bodyContent;

  private Body(Builder builder) {
    this.bodyContent = builder.bodyContent;
  }

  /**
   * Getter.
   * @return the contents.
   */
  public BodyContent getContents() {
    return bodyContent;
  }

  @Override
  public Object asJson() {
    HashMap<String, Object> values = new HashMap<>();

    if (bodyContent != null) {
      values.put(bodyContent.getKeyName(), bodyContent);
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

    Body body = (Body) o;

    return bodyContent != null ? bodyContent.equals(body.bodyContent) : body.bodyContent == null;
  }

  @Override
  public int hashCode() {
    return bodyContent != null ? bodyContent.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Body{"
        + "bodyContent=" + bodyContent
        + '}';
  }

  /**
   * Builder class for {@link Body body}.
   */
  public static final class Builder {

    private BodyContent bodyContent;

    /**
     * Constructor.
     */
    public Builder() {

    }

    /**
     * Constructor.
     *
     * @param body the {@link Body body} to initialize a new builder instance.
     */
    public Builder(Body body) {
      this.bodyContent = body.bodyContent;
    }

    /**
     * The contents of this body (either {@link Trace}, {@link TraceChain}, {@link Message}, or
     * {@link CrashReport}).
     *
     * @param bodyContent the body content;
     * @return the builder instance.
     */
    public Builder bodyContent(BodyContent bodyContent) {
      this.bodyContent = bodyContent;
      return this;
    }

    /**
     * Builds the {@link Body body}.
     *
     * @return the body.
     */
    public Body build() {
      return new Body(this);
    }
  }
}
