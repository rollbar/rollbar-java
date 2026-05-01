package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;

/**
 * Records network telemetry events and errors for HTTP requests.
 */
public interface NetworkTelemetryRecorder {
  /**
   * Records a completed network request as a telemetry event.
   *
   * @param level      the severity level to attach to the telemetry event
   * @param method     the HTTP method (e.g. GET, POST)
   * @param url        the request URL with userinfo (basic-auth credentials), query parameters,
   *                   and fragment stripped by default; supply a custom sanitizer to
   *                   {@link RollbarOkHttpInterceptor} to change this behavior
   * @param statusCode the HTTP response status code as a string (e.g. "200", "404")
   */
  void recordNetworkEvent(Level level, String method, String url, String statusCode);

  /**
   * Records a network error event when an HTTP request fails with an exception.
   *
   * @param exception the exception thrown during the request
   */
  void recordErrorEvent(Exception exception);
}
