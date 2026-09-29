package com.rollbar.api.scrubbing;

import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultUrlSanitizerTest {

  private final DefaultUrlSanitizer sanitizer = DefaultUrlSanitizer.INSTANCE;

  @Test
  public void nullInputReturnsNull() {
    assertNull(sanitizer.sanitize(null));
  }

  @Test
  public void cleanUrlUnchanged() {
    String url = "https://example.com/api/v1/things";
    assertEquals(url, sanitizer.sanitize(url));
  }

  @Test
  public void queryStringStripped() {
    assertEquals(
        "https://example.com/search",
        sanitizer.sanitize("https://example.com/search?token=abc&page=1")
    );
  }

  @Test
  public void fragmentStripped() {
    assertEquals(
        "https://example.com/page",
        sanitizer.sanitize("https://example.com/page#section")
    );
  }

  @Test
  public void userinfoStripped() {
    assertEquals(
        "https://example.com/path",
        sanitizer.sanitize("https://user:pass@example.com/path")
    );
  }

  @Test
  public void allThreeScrubbed() {
    assertEquals(
        "https://example.com/path",
        sanitizer.sanitize("https://admin:secret@example.com/path?token=xyz#top")
    );
  }

  @Test
  public void malformedUrlNoException() {
    // Should not throw; best-effort strip
    String result = sanitizer.sanitize("not-a-url?query=sensitive");
    assertNotNull(result);
    assertFalse(result.contains("sensitive"));
  }

  @Test
  public void malformedUrlWithUserinfo() {
    String result = sanitizer.sanitize("http://user:secret@host/path?q=1");
    assertNotNull(result);
    assertFalse(result.contains("secret"));
    assertFalse(result.contains("q=1"));
  }

  @Test
  public void emptyStringUnchanged() {
    assertEquals("", sanitizer.sanitize(""));
  }

  @Test
  public void cleanUrlReturnedAsSameInstance() {
    String url = "https://example.com/api/v1/things";
    assertSame(url, sanitizer.sanitize(url));
  }

  @Test
  public void percentEncodedPathPreserved() {
    // No ?, #, or @ — fast path must return the same instance without normalizing encoding.
    String url = "https://example.com/path%20with%20spaces";
    assertSame(url, sanitizer.sanitize(url));
  }

  @Test
  public void atSignInPathNotTreatedAsUserinfo() {
    // The @ is after the first path slash, so it is not userinfo.
    String url = "https://example.com/users/@alice?token=x";
    String result = sanitizer.sanitize(url);
    assertTrue(result.contains("@alice"));
    assertFalse(result.contains("token"));
  }
}
