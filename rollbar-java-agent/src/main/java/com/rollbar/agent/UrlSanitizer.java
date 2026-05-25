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
      return rawUrl;
    }
  }
}
