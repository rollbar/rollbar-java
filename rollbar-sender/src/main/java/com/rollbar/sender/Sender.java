package com.rollbar.sender;

import com.rollbar.payload.Payload;

/**
 * Senders can send JSON string payloads to Rollbar.
 */
public interface Sender {
    /**
     * Send the payload to Rollbar returning the response
     * @param payload the payload to send
     * @return a {@link RollbarResponse} indicating what happened.
     */
    RollbarResponse send(Payload payload);

    /**
     * Send the json payload, handle the response with an object
     * @param payload the payload being sent
     * @param handler an object that can handle a rollbar response handler
     */
    void send(Payload payload, RollbarResponseHandler handler);
}
