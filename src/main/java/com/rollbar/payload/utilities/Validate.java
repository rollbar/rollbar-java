package com.rollbar.payload.utilities;

/**
 * Validates Strings
 * Created by chris on 11/10/15.
 */
public class Validate {
    public static void isNotNullOrWhitespace(String x, String name) throws ArgumentNullException {
        if (x == null || x.isEmpty()) {
            throw new ArgumentNullException(name);
        }
    }

    public static void maxLength(String x, int max, String name) throws InvalidLengthException {
        if (x.length() > max) {
            throw InvalidLengthException.TooLong(name, max);
        }
    }

    public static <T> void minLength(T[] x, int min, String name) throws InvalidLengthException {
        if (x.length < min) {
            throw InvalidLengthException.TooShort(name, min);
        }
    }

    public static <T> void isNotNull(T data, String name) throws ArgumentNullException {
        if (data == null) {
            throw new ArgumentNullException(name);
        }
    }
}
