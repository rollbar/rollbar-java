package com.rollbar.http;

/**
 * The RollbarResponse is the response received from Rollbar after sending a report.
 */
public class RollbarResponse {
    private final RollbarResponseCode statusCode;
    private final String result;

    /**
     * The static factory method for a successful post to Rollbar
     * @param uuid the occurrence id that will result from the POST
     * @return the RollbarResponse
     */
    public static RollbarResponse success(String uuid) {
        return new RollbarResponse(RollbarResponseCode.Success, uuid);
    }

    /**
     * The static factory method for a failed post to Rollbar
     * @param code the RollbarResponseCode that caused this
     * @param reason the message explaining the failure
     * @return the RollbarResponse
     */
    public static RollbarResponse failure(RollbarResponseCode code, String reason) {
        return new RollbarResponse(code, reason);
    }

    /**
     * The static factory method for a payload that wasn't sent because it was filtered.
     * @return a RollbarResponse indicating the payload wasn't sent
     */
    public static RollbarResponse notSent() {
        return new RollbarResponse(RollbarResponseCode.Filtered, "Payload not sent, because it was filtered.");
    }

    private RollbarResponse(RollbarResponseCode statusCode, String result) {
        this.statusCode = statusCode;
        this.result = result;
    }

    /**
     * Get the HTTP status code (as a RollbarResponseCode).
     * @return the response code
     */
    public RollbarResponseCode statusCode() {
        return this.statusCode;
    }

    /**
     * Get the UUID of the occurrence, if the post was successful. <code>null</code> otherwise.
     * @return the uuid of the occurrence that happened because of the request.
     */
    public String uuid() {
        if (isSuccessful()) {
            return result;
        }
        return null;
    }

    /**
     * Get the URL of the instance, given the UUID returned by Rollbar.
     * @return the url to the instance on the Rollbar website.
     */
    public String instanceUrl() {
        final String urlFormat = "https://rollbar.com/instance/uuid/?uuid=%s";
        if (isSuccessful()) {
            return String.format(urlFormat, uuid());
        }
        return null;
    }

    /**
     * Get the error message returned by rollbar if not successful. <code>null</code> otherwise.
     * @return the error message
     */
    public String errorMessage() {
        if (!isSuccessful()) {
            return result;
        }
        return null;
    }

    /**
     * @return whether or not the RollbarResponse represents a successful POST to Rollbar.
     */
    public boolean isSuccessful() {
        return statusCode == RollbarResponseCode.Success;
    }
}
