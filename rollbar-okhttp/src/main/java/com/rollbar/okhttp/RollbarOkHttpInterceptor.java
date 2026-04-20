package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;

import java.io.IOException;
import java.util.logging.Logger;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RollbarOkHttpInterceptor implements Interceptor {

  private static final Logger LOGGER = Logger.getLogger(RollbarOkHttpInterceptor.class.getName());

  private final NetworkTelemetryRecorder recorder;

  public RollbarOkHttpInterceptor(NetworkTelemetryRecorder recorder) {
    this.recorder = recorder;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();

    try {
      Response response = chain.proceed(request);

      if (response.code() >= 400 && recorder != null) {
        try {
          recorder.recordNetworkEvent(
              Level.CRITICAL,
              request.method(),
              request.url().toString(),
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
