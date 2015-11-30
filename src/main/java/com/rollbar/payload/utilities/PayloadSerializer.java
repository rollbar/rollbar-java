package com.rollbar.payload.utilities;

import com.rollbar.payload.Payload;

/**
 * The interface that encapsulates turning a {@link Payload} into a JSON payload string.
 */
public interface PayloadSerializer {
    /**
     * Turn the {@link Payload} into a json string
     * @param payload the {@link Payload} to serialize
     * @return the json payload.
     */
    String serialize(Payload payload);
}
