package com.rollbar.http;

/**
 * Created by chris on 11/13/15.
 */
public class InvalidResponseCodeException extends Exception {
    private int value;

    public InvalidResponseCodeException(int value) {
        super(String.format("%d is an unknown response code"));
        this.value = value;
    }
}
