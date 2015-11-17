package com.rollbar.http;

public class RollbarResponse {
    private final RollbarResponseCode statusCode;
    private final String result;

    public static RollbarResponse success(String uuid) {
        return new RollbarResponse(RollbarResponseCode.Success, uuid);
    }

    public static RollbarResponse failure(RollbarResponseCode code, String reason) {
        return new RollbarResponse(code, reason);
    }

    private RollbarResponse(RollbarResponseCode statusCode, String result) {
        this.statusCode = statusCode;
        this.result = result;
    }

    public RollbarResponseCode statusCode() {
        return this.statusCode;
    }

    public String uuid() {
        if (isSuccessful()) {
            return result;
        }
        return null;
    }

    public String errorMessage() {
        if (!isSuccessful()) {
            return result;
        }
        return null;
    }

    public boolean isSuccessful() {
        return statusCode == RollbarResponseCode.Success;
    }
}
