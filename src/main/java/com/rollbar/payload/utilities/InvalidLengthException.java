package com.rollbar.payload.utilities;

public class InvalidLengthException extends IllegalArgumentException {
    public static InvalidLengthException TooLong(String parameter, int len) {
        final String msgFmt = "%s too long (over %d)";
        return new InvalidLengthException(String.format(msgFmt, parameter, len));
    }

    public static InvalidLengthException TooShort(String parameter, int len) {
        final String msgFmt = "%s too short (under %d)";
        return new InvalidLengthException(String.format(msgFmt, parameter, len));
    }

    private InvalidLengthException(String message) {
        super(message);
    }
}
