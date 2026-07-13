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
      // Use getAuthority() rather than getHost(): URI parses authorities that fail RFC 2396
      // server-based grammar (e.g. underscores in Kubernetes/AD internal DNS) in registry-based
      // mode, where getHost() returns null and the host would be silently dropped. Strip userinfo
      // from the authority manually.
      String authority = uri.getAuthority();
      if (authority != null) {
        int at = authority.indexOf('@');
        if (at >= 0) {
          authority = authority.substring(at + 1);
        }
      }
      return new URI(uri.getScheme(), authority, uri.getPath(), null, null).toString();
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

    // Drop userinfo: scheme://user:pass@host/path → scheme://host/path. Bound the '@' search to
    // the authority component (before the first '/', '?', or '#') so an '@' inside the path — e.g.
    // /@handle or /@scope/pkg — is not mistaken for the userinfo separator, which would delete the
    // real host and promote a path segment to host.
    int schemeEnd = result.indexOf("://");
    if (schemeEnd >= 0) {
      int authorityStart = schemeEnd + 3;
      int authorityEnd = result.length();
      for (int i = authorityStart; i < result.length(); i++) {
        char c = result.charAt(i);
        if (c == '/' || c == '?' || c == '#') {
          authorityEnd = i;
          break;
        }
      }
      int atSign = result.indexOf('@', authorityStart);
      if (atSign >= 0 && atSign < authorityEnd) {
        result = result.substring(0, authorityStart) + result.substring(atSign + 1);
      }
    }
    return result;
  }
}
