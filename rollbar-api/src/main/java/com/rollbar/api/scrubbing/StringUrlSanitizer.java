package com.rollbar.api.scrubbing;

/**
 * Sanitizes a URL string before it is included in a Rollbar payload.
 * Implementations should strip sensitive components such as userinfo,
 * query parameters, and fragments.
 */
@FunctionalInterface
public interface StringUrlSanitizer {
  /**
   * Returns a sanitized version of the given URL string, or {@code null} if
   * the input is {@code null}.
   *
   * @param url the raw URL string, may be {@code null}.
   * @return the sanitized URL, or {@code null}.
   */
  String sanitize(String url);
}
