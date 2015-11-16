package com.rollbar.payload.utilities;

public class ArgumentNullException extends Exception {
    public ArgumentNullException(String parameter) {
        super(String.format("'%s' cannot be null", parameter));
    }
}
