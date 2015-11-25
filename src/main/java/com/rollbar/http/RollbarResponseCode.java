package com.rollbar.http;

public enum RollbarResponseCode {
    Success(200),
    BadRequest(400),
    Unauthorized(401),
    AccessDenied(403),
    RequestTooLarge(413),
    UnprocessablePayload(422),
    TooManyRequests(429),
    InternalServerError(500);

    private final int value;

    RollbarResponseCode(int value) {
        this.value = value;
    }

    public RollbarResponse response(String message) {
        return RollbarResponse.failure(this, message);
    }

    public static RollbarResponseCode fromInt(int i) throws InvalidResponseCodeException {
        for(RollbarResponseCode rrc : RollbarResponseCode.values()) {
            if (rrc.value == i) return rrc;
        }
        throw new InvalidResponseCodeException(i);
    }
}
