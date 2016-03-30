package com.rollbar.sender;

public interface RollbarResponseHandler {
    void handleResponse(RollbarResponse response);
}
