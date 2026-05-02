package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollbarOkHttpInterceptor implements Interceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(RollbarOkHttpInterceptor.class);

  private static final UrlSanitizer DEFAULT_URL_SANITIZER =
      url -> url
              .newBuilder()
              .username("")
              .password("")
              .query(null)
              .fragment(null)
              .build()
              .toString();

  private final NetworkTelemetryRecorder recorder;
  private final UrlSanitizer urlSanitizer;

  public RollbarOkHttpInterceptor(NetworkTelemetryRecorder recorder) {
    this(recorder, DEFAULT_URL_SANITIZER);
  }

  public RollbarOkHttpInterceptor(
          NetworkTelemetryRecorder recorder,
          UrlSanitizer urlSanitizer) {
    this.recorder = recorder;
    this.urlSanitizer = Objects.requireNonNull(urlSanitizer, "urlSanitizer must not be null");
  }

  @NotNull
  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();

    try {
      Response response = chain.proceed(request);

      if (response.code() >= 400 && recorder != null) {
        String sanitizedUrl;
        try {
          sanitizedUrl = urlSanitizer.sanitize(request.url());
        } catch (Exception sanitizerException) {
          LOGGER.warn("urlSanitizer threw an exception; "
              + "suppressing to preserve the interceptor contract.", sanitizerException);
          return response;
        }
        try {
          recorder.recordNetworkEvent(
              Level.CRITICAL,
              request.method(),
              sanitizedUrl,
              String.valueOf(response.code()));
        } catch (Exception recorderException) {
          LOGGER.warn("NetworkTelemetryRecorder.recordNetworkEvent threw an exception; "
              + "suppressing to preserve the interceptor contract.", recorderException);
        }
      }

      return response;

    } catch (IOException e) {
      if (recorder != null) {
        try {
          recorder.recordErrorEvent(e);
        } catch (Exception recorderException) {
          LOGGER.warn("NetworkTelemetryRecorder.recordErrorEvent threw an exception; "
              + "suppressing to preserve the original IOException.", recorderException);
        }
      }

      throw e;
    }
  }
}
