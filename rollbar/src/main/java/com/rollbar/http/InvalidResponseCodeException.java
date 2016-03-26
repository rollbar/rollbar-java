package com.rollbar.http;

/**
 * Represents an unexpected return value from the Rollbar API.
 * Expected values are declared in {@link RollbarResponseCode}.
 */
public class InvalidResponseCodeException extends Exception {
    private final int value;

    /**
     * Constructor
     * @param value the unexpected value
     */
    public InvalidResponseCodeException(int value) {
        super(String.format("%d is an unknown response code", value));
        this.value = value;
    }

    /**
     * Get the unexpected value.
     * @return the value that wasn't a known {@link RollbarResponseCode}
     */
    public int value() {
        return this.value;
    }
}
