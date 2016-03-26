package com.rollbar.http;

public interface RollbarResponseHandler {
    void handleResponse(RollbarResponse response);
}
