package com.rollbar.payload.data.body;

import com.google.gson.annotations.SerializedName;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

public class ExceptionInfo {
    public static ExceptionInfo fromThrowable(Throwable error) throws ArgumentNullException {
        Validate.isNotNull(error, "error");
        String className = error.getClass().getSimpleName();
        String message = error.getMessage();
        return new ExceptionInfo(className, message, null);
    }

    @SerializedName("class")
    private final String className;
    private final String message;
    private final String description;


    public ExceptionInfo(String className) throws ArgumentNullException {
        this(className, null, null);
    }

    public ExceptionInfo(String className, String message, String description) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(className, "className");
        this.className = className;
        this.message = message;
        this.description = description;
    }

    public String className() {
        return this.className;
    }

    public ExceptionInfo className(String className) throws ArgumentNullException {
        return new ExceptionInfo(className, message, description);
    }

    public String message() {
        return this.message;
    }

    public ExceptionInfo message(String message) {
        try {
            return new ExceptionInfo(className, message, description);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("className shouldn't be null");
        }
    }

    public String description() {
        return this.description;
    }

    public ExceptionInfo description(String description) {
        try {
            return new ExceptionInfo(className, message, description);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("className shouldn't be null");
        }
    }
}
