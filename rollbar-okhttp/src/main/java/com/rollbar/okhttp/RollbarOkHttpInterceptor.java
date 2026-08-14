package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.scrubbing.DefaultUrlSanitizer;
import com.rollbar.api.scrubbing.StringUrlSanitizer;

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
      url -> DefaultUrlSanitizer.INSTANCE.sanitize(url.toString());

  private final NetworkTelemetryRecorder recorder;
  private final UrlSanitizer urlSanitizer;

  /**
   * Creates an interceptor that sanitizes URLs with the same {@link StringUrlSanitizer} used by
   * the notifier configuration, so both paths redact identically.
   *
   * <p>This is a static factory rather than a constructor overload because {@link UrlSanitizer}
   * and {@link StringUrlSanitizer} are both functional interfaces: overloaded constructors would
   * make a lambda argument ambiguous and break existing callers.
   *
   * @param recorder the telemetry recorder.
   * @param sanitizer the sanitizer shared with the notifier config.
   * @return the interceptor.
   */
  public static RollbarOkHttpInterceptor withSharedUrlSanitizer(NetworkTelemetryRecorder recorder,
      StringUrlSanitizer sanitizer) {
    Objects.requireNonNull(sanitizer, "sanitizer must not be null");
    return new RollbarOkHttpInterceptor(recorder, url -> sanitizer.sanitize(url.toString()));
  }

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
