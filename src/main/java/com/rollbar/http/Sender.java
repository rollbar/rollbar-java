package com.rollbar.http;

import com.rollbar.payload.Payload;

/**
 * Senders can send JSON string payloads to Rollbar.
 */
public interface Sender {
    /**
     * Send the payload to Rollbar
     * @param payload the payload to send
     * @return a {@link RollbarResponse} indicating what happened.
     * @throws ConnectionFailedException if the connection failed before receiving a response from Rollbar.
     */
    RollbarResponse send(Payload payload);

    /**
     * Send the json payload (already serialized
     * @param payload
     * @param handler an object that can handle a rollbar response handler
     * @throws ConnectionFailedException
     */
    void send(Payload payload, RollbarResponseHandler handler);
}
