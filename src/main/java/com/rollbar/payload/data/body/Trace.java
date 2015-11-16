package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

public class Trace implements BodyContents {
    public static Trace fromThrowable(Throwable error) throws ArgumentNullException {
        Validate.isNotNull(error, "error");

        Frame[] frames = Frame.fromThrowable(error);
        ExceptionInfo exceptionInfo = ExceptionInfo.fromThrowable(error);

        return new Trace(frames, exceptionInfo);
    }

    private final Frame[] frames;
    private final ExceptionInfo exception;

    public Trace(Frame[] frames, ExceptionInfo exception) throws ArgumentNullException {
        Validate.isNotNull(frames, "frames");
        this.frames = frames.clone();
        Validate.isNotNull(exception, "exception");
        this.exception = exception;
    }

    public Frame[] frames() {
        return this.frames.clone();
    }

    public Trace frames(Frame[] frames) throws ArgumentNullException {
        return new Trace(frames, exception);
    }

    public ExceptionInfo exception() {
        return this.exception;
    }

    public Trace exception(ExceptionInfo exception) throws ArgumentNullException {
        return new Trace(frames, exception);
    }
}
