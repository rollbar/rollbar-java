package com.rollbar.http;

public class InvalidResponseCodeException extends Exception {
    private final int value;

    public InvalidResponseCodeException(int value) {
        super(String.format("%d is an unknown response code", value));
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
