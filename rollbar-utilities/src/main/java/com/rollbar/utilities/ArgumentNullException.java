package com.rollbar.utilities;

/**
 * Indicates a null argument was passed when it shouldn't have been.
 */
public class ArgumentNullException extends IllegalArgumentException {
    /**
     * Constructor
     * @param parameter the null parameter
     */
    public ArgumentNullException(String parameter) {
        super(String.format("'%s' cannot be null", parameter));
    }
}
