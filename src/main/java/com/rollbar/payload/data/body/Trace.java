package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

/**
 * Represent a Stack Trace to send to Rollbar
 */
public class Trace implements BodyContents {
    /**
     * Create a stack trace from a throwable
     * @param error the Throwable to create a stack trace from
     * @return the Trace representing the Throwable
     * @throws ArgumentNullException if error is null
     */
    public static Trace fromThrowable(Throwable error) throws ArgumentNullException {
        Validate.isNotNull(error, "error");

        Frame[] frames = Frame.fromThrowable(error);
        ExceptionInfo exceptionInfo = ExceptionInfo.fromThrowable(error);

        return new Trace(frames, exceptionInfo);
    }

    private final Frame[] frames;
    private final ExceptionInfo exception;

    /**
     * Constructor
     * @param frames not nullable, frames making up the exception
     * @param exception not nullable, info about the exception
     * @throws ArgumentNullException if either argument is null
     */
    public Trace(Frame[] frames, ExceptionInfo exception) throws ArgumentNullException {
        Validate.isNotNull(frames, "frames");
        this.frames = frames.clone();
        Validate.isNotNull(exception, "exception");
        this.exception = exception;
    }

    /**
     * @return a copy of the frames
     */
    public Frame[] frames() {
        return this.frames.clone();
    }

    /**
     * Set frames on a copy of this Trace
     * @param frames the frames to set
     * @return a copy of this Trace with frames overridden
     * @throws ArgumentNullException if frames are null
     */
    public Trace frames(Frame[] frames) throws ArgumentNullException {
        return new Trace(frames, exception);
    }

    /**
     * @return the exception info
     */
    public ExceptionInfo exception() {
        return this.exception;
    }

    /**
     * Set the exception on a copy of this Trace
     * @param exception the new exception info
     * @return a copy of this Trace with exception set
     * @throws ArgumentNullException if exception is null
     */
    public Trace exception(ExceptionInfo exception) throws ArgumentNullException {
        return new Trace(frames, exception);
    }
}
