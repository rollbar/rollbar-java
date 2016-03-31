package com.rollbar.utilities;

/**
 * The interface that encapsulates turning a {@link JsonSerializable} into a JSON payload string.
 */
public interface JsonSerializer {
    /**
     * Turn the {@link JsonSerializable} into a json string
     * @param payload the {@link JsonSerializable} to serialize
     * @return the json payload.
     */
    String serialize(JsonSerializable payload);
}
