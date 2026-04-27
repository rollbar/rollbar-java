package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;

public interface NetworkTelemetryRecorder {
  /**
   * @param url the request URL with query parameters stripped by default; supply a custom
   *            sanitizer to {@link RollbarOkHttpInterceptor} to change this behavior.
   */
  void recordNetworkEvent(Level level, String method, String url, String statusCode);

  void recordErrorEvent(Exception exception);
}
