package com.rollbar.http;

/**
 * Represents expected response codes from POSTing an item to Rollbar
 *
 */
public enum RollbarResponseCode {
    /**
     * A successful POST to the API
     */
    Success(200),
    /**
     * Invalid, or missing, JSON POST body.
     */
    BadRequest(400),
    /**
     * Missing Access Token. (Check your JSON serialization).
     */
    Unauthorized(401),
    /**
     * Invalid access token. Bad format, invalid scope, for type of request (e.g. posting to post_client_token,
     * for server side error).
     * See the {@link RollbarResponse} message.
     */
    AccessDenied(403),
    /**
     * Max Payload size exceeded. Try removing or truncating particularly large portions of your payload
     * (e.g. whole binary files, or large strings).
     */
    RequestTooLarge(413),
    /**
     * JSON was valid, but semantically incorrect. See {@link RollbarResponse} message.
     */
    UnprocessablePayload(422),
    /**
     * Rate limit for your account or access token was reached.
     */
    TooManyRequests(429),
    /**
     * An error occurred on Rollbar's end.
     */
    InternalServerError(500),

    /**
     * Indicates that the Payload was not sent to Rollbar because it was filtered
     */
    Filtered(-1),

    /**
     * Indicates that the Payload was not sent to Rollbar because the connection failed
     */
    ConnectionFailed(-2);

    private final int value;

    RollbarResponseCode(int value) {
        this.value = value;
    }

    /**
     * Create a {@link RollbarResponse} from a response code, with a message.
     * @param message The explanatory message.
     * @return RollbarResponse with explanatory message.
     */
    public RollbarResponse response(String message) {
        return RollbarResponse.failure(this, message);
    }

    /**
     * Get a Response Code from the integer value returned by the HTTP request.
     * @param i the integer value of the response code.
     * @return the RollbarResponseCode
     * @throws InvalidResponseCodeException if not a valid ResponseCode
     */
    public static RollbarResponseCode fromInt(int i) throws InvalidResponseCodeException {
        for(RollbarResponseCode rrc : RollbarResponseCode.values()) {
            if (rrc.value == i) return rrc;
        }
        throw new InvalidResponseCodeException(i);
    }
}
