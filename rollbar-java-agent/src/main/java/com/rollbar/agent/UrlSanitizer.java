package com.rollbar.agent;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlSanitizer {

  private UrlSanitizer() {}

  /**
   * Strips userinfo, query parameters, and fragment from the URL, leaving only
   * scheme, host, port, and path.
   */
  public static String sanitize(String rawUrl) {
    if (rawUrl == null) {
      return null;
    }
    try {
      URI uri = new URI(rawUrl);
      return new URI(
          uri.getScheme(),
          null,
          uri.getHost(),
          uri.getPort(),
          uri.getPath(),
          null,
          null
      ).toString();
    } catch (URISyntaxException e) {
      return fallbackSanitize(rawUrl);
    }
  }

  // URI rejected the URL (e.g. unescaped space, bad percent-escape). Strip query, fragment, and
  // userinfo with plain string ops rather than failing open with the raw URL.
  private static String fallbackSanitize(String rawUrl) {
    // Drop query string and fragment — take everything before the first '?' or '#'.
    int end = rawUrl.length();
    int query = rawUrl.indexOf('?');
    int header = rawUrl.indexOf('#');
    if (query >= 0) {
      end = query;
    }
    if (header >= 0) {
      end = Math.min(end, header);
    }
    String result = rawUrl.substring(0, end);

    // Drop userinfo: scheme://user:pass@host/path → scheme://host/path
    int schemeEnd = result.indexOf("://");
    if (schemeEnd >= 0) {
      int atSign = result.indexOf('@', schemeEnd + 3);
      if (atSign >= 0) {
        result = result.substring(0, schemeEnd + 3) + result.substring(atSign + 1);
      }
    }
    return result;
  }
}
