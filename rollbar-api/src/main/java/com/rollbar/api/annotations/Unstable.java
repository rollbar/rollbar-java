package com.rollbar.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * Classes and methods annotated with {@link Unstable} will likely change in ways that will break
 * backwards compatibility, both at source and binary level.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Unstable {
  String reason = "This class or method is not part of the SDK's public API, and will likely "
      + "change in ways that will break backwards compatibility, both at source and binary level.";
}
