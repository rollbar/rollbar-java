package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

import java.util.HashMap;

public class Body {
    public static Body fromError(Throwable error) throws ArgumentNullException {
        Validate.isNotNull(error, "error");
        if (error.getCause() == null) {
            return Body.trace(error);
        } else {
            return Body.traceChain(error);
        }
    }

    public static Body traceChain(Throwable error) throws ArgumentNullException {
        final TraceChain chain = TraceChain.fromThrowable(error);
        return new Body(chain);
    }

    public static Body trace(Throwable error) throws ArgumentNullException {
        final Trace trace = Trace.fromThrowable(error);
        return new Body(trace);
    }

    public static Body message(String message) throws ArgumentNullException {
        return Body.message(message, null);
    }

    public static Body message(String message, HashMap<String, Object> extra) throws ArgumentNullException {
        final BodyContents contents = new Message(message, extra);
        return new Body(contents);
    }

    public static Body crashReport(String raw) throws ArgumentNullException {
        final CrashReport contents = new CrashReport(raw);
        return new Body(contents);
    }

    private final BodyContents contents;

    public Body(BodyContents contents) throws ArgumentNullException {
        Validate.isNotNull(contents, "contents");
        this.contents = contents;
    }

    public BodyContents contents() {
        return contents;
    }

    public Body contents(BodyContents contents) throws ArgumentNullException {
        return new Body(contents);
    }
}
