package com.rollbar.utilities;

/**
 * An IllegalArgumentException indicating an argument that's too long or too short.
 */
public class InvalidLengthException extends IllegalArgumentException {

    private InvalidLengthException(String message) {
        super(message);
    }

    /**
     * Static Factory making an exception indicating an argument was passed that was too long.
     * @param parameter the parameter that was too long
     * @param len the maximum length
     * @return the exception
     */
    public static InvalidLengthException TooLong(String parameter, int len) {
        final String msgFmt = "%s too long (over %d)";
        return new InvalidLengthException(String.format(msgFmt, parameter, len));
    }

    /**
     * Static Factory making an exception indicating an argument was passed that was too short.
     * @param parameter the parameter that was too short
     * @param len the minimum length
     * @return the exception
     */
    public static InvalidLengthException TooShort(String parameter, int len) {
        final String msgFmt = "%s too short (under %d)";
        return new InvalidLengthException(String.format(msgFmt, parameter, len));
    }

}
