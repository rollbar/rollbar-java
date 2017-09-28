package com.rollbar.notifier.sender.result;

/**
 * Represents expected response codes from POSTing an item to Rollbar.
 */
public enum ResultCode {

  /**
   * A successful POST to the API.
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
   * Invalid access token. Bad format, invalid scope, for type of request (e.g. posting to
   * post_client_token, for server side error). See the {@link Result} message.
   */
  AccessDenied(403),
  /**
   * Max Payload size exceeded. Try removing or truncating particularly large portions of your
   * payload (e.g. whole binary files, or large strings).
   */
  RequestTooLarge(413),
  /**
   * JSON was valid, but semantically incorrect. See {@link Result} message.
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

  Unknown(-500);

  private final int value;

  ResultCode(int value) {
    this.value = value;
  }

  /**
   * Get a Response Code from the integer value returned by the HTTP request.
   *
   * @param i the integer value of the response code.
   * @return the ResultCode.
   */
  public static ResultCode fromInt(int i) {
    for (ResultCode rrc : ResultCode.values()) {
      if (rrc.value == i) {
        return rrc;
      }
    }
    return Unknown;
  }
}
