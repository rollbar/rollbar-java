package com.rollbar.api.scrubbing;

/**
 * Default {@link StringUrlSanitizer} that strips userinfo, query string, and fragment from URLs.
 * Uses string scanning rather than {@code java.net.URI} to avoid allocation on clean URLs
 * and to preserve the original percent-encoding without normalization.
 */
public final class DefaultUrlSanitizer implements StringUrlSanitizer {

  public static final DefaultUrlSanitizer INSTANCE = new DefaultUrlSanitizer();

  private DefaultUrlSanitizer() {
  }

  @Override
  public String sanitize(String url) {
    if (url == null) {
      return null;
    }
    // Fast path: no characters that can introduce query string, fragment, or userinfo.
    if (url.indexOf('?') < 0 && url.indexOf('#') < 0 && url.indexOf('@') < 0) {
      return url;
    }
    return strip(url);
  }

  private static String strip(String url) {
    int end = url.length();
    int q = url.indexOf('?');
    int f = url.indexOf('#');
    if (q >= 0 && q < end) {
      end = q;
    }
    if (f >= 0 && f < end) {
      end = f;
    }
    // Strip userinfo: find "://" then the last "@" before the first "/" after the authority start.
    String result = url.substring(0, end);
    int schemeEnd = result.indexOf("://");
    if (schemeEnd >= 0) {
      int hostStart = schemeEnd + 3;
      int slashAfterHost = result.indexOf('/', hostStart);
      int searchEnd = slashAfterHost < 0 ? result.length() : slashAfterHost;
      int at = result.lastIndexOf('@', searchEnd);
      if (at >= hostStart) {
        result = result.substring(0, hostStart) + result.substring(at + 1);
      }
    }
    return result;
  }
}
