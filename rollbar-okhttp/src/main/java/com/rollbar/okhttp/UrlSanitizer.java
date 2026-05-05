package com.rollbar.okhttp;

import okhttp3.HttpUrl;

@FunctionalInterface
public interface UrlSanitizer {
  String sanitize(HttpUrl url);
}
