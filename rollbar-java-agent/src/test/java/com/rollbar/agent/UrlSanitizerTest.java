package com.rollbar.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UrlSanitizerTest {

  @Test
  public void normalUrl_stripsQueryAndFragment() {
    assertEquals(
        "https://example.com/path",
        UrlSanitizer.sanitize("https://example.com/path?token=secret&key=value")
    );
  }

  @Test
  public void normalUrl_stripsUserinfo() {
    assertEquals(
        "https://example.com/path",
        UrlSanitizer.sanitize("https://user:pass@example.com/path")
    );
  }

  @Test
  public void normalUrl_stripsFragment() {
    assertEquals(
        "https://example.com/path",
        UrlSanitizer.sanitize("https://example.com/path#section")
    );
  }

  @Test
  public void urlWithUnescapedSpace_fallback_stripsQuery() {
    // URI rejects unescaped spaces; fallback must still strip the query string.
    String result = UrlSanitizer.sanitize("http://example.com/path with spaces?token=secret");
    assertFalse(result.contains("token"), "query must be stripped even on parse failure");
    assertFalse(result.contains("secret"), "secret value must be stripped even on parse failure");
    assertTrue(result.contains("example.com"), "host should be preserved");
  }

  @Test
  public void urlWithUnescapedSpace_fallback_stripsUserinfo() {
    String result = UrlSanitizer.sanitize("http://user:pass@example.com/path with spaces");
    assertFalse(result.contains("pass"), "userinfo must be stripped even on parse failure");
    assertTrue(result.contains("example.com"), "host should be preserved");
  }

  @Test
  public void urlWithBadPercentEncoding_fallback_stripsQuery() {
    String result = UrlSanitizer.sanitize("http://example.com/path%zz?secret=abc");
    assertFalse(result.contains("secret"), "query must be stripped even on parse failure");
  }

  @Test
  public void nullUrl_returnsNull() {
    assertNull(UrlSanitizer.sanitize(null));
  }
}
