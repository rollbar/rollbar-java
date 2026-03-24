package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RollbarOkHttpInterceptor implements Interceptor {

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
        recorder.recordNetworkEvent(
            Level.CRITICAL,
            request.method(),
            request.url().toString(),
            String.valueOf(response.code()));
      }

      return response;

    } catch (IOException e) {
      if (recorder != null) {
        recorder.recordErrorEvent(e);
      }

      throw e;
    }
  }
}
