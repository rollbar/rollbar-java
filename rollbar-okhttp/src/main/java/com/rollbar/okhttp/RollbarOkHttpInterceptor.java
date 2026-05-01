package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RollbarOkHttpInterceptor implements Interceptor {

  private static final Logger LOGGER = Logger.getLogger(RollbarOkHttpInterceptor.class.getName());

  private static final Function<HttpUrl, String> DEFAULT_URL_SANITIZER =
      url -> url.newBuilder().username("").password("").query(null).fragment(null).build().toString();

  private final NetworkTelemetryRecorder recorder;
  private final Function<HttpUrl, String> urlSanitizer;

  public RollbarOkHttpInterceptor(NetworkTelemetryRecorder recorder) {
    this(recorder, DEFAULT_URL_SANITIZER);
  }

  public RollbarOkHttpInterceptor(
          NetworkTelemetryRecorder recorder,
          Function<HttpUrl, String> urlSanitizer) {
    this.recorder = recorder;
    this.urlSanitizer = Objects.requireNonNull(urlSanitizer, "urlSanitizer must not be null");
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();

    try {
      Response response = chain.proceed(request);

      if (response.code() >= 400 && recorder != null) {
        String sanitizedUrl;
        try {
          sanitizedUrl = urlSanitizer.apply(request.url());
        } catch (Exception sanitizerException) {
          LOGGER.log(java.util.logging.Level.WARNING,
              "urlSanitizer threw an exception; "
                  + "suppressing to preserve the interceptor contract.",
              sanitizerException);
          return response;
        }
        try {
          recorder.recordNetworkEvent(
              Level.CRITICAL,
              request.method(),
              sanitizedUrl,
              String.valueOf(response.code()));
        } catch (Exception recorderException) {
          LOGGER.log(java.util.logging.Level.WARNING,
              "NetworkTelemetryRecorder.recordNetworkEvent threw an exception; "
                  + "suppressing to preserve the interceptor contract.",
              recorderException);
        }
      }

      return response;

    } catch (IOException e) {
      if (recorder != null) {
        try {
          recorder.recordErrorEvent(e);
        } catch (Exception recorderException) {
          LOGGER.log(java.util.logging.Level.WARNING,
              "NetworkTelemetryRecorder.recordErrorEvent threw an exception; "
                  + "suppressing to preserve the original IOException.",
              recorderException);
        }
      }

      throw e;
    }
  }
}
