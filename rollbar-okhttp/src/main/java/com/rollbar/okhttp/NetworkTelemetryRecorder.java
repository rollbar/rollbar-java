package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;

public interface NetworkTelemetryRecorder {
    void recordNetworkEvent(Level level, String method, String url, String statusCode);
    void recordErrorEvent(Exception exception);
}
