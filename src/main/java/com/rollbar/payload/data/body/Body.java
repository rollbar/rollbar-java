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

    private static Body traceChain(Throwable error) throws ArgumentNullException {
        final TraceChain chain = TraceChain.fromThrowable(error);
        return new Body(chain);
    }

    private static Body trace(Throwable error) throws ArgumentNullException {
        final Trace trace = Trace.fromThrowable(error);
        return new Body(trace);
    }

    public static Body fromString(String message) throws ArgumentNullException {
        return Body.fromString(message, null);
    }

    public static Body fromString(String message, HashMap<String, Object> extra) throws ArgumentNullException {
        final BodyContents contents = new Message(message, extra);
        return new Body(contents);
    }

    public static Body fromCrashReportString(String raw) throws ArgumentNullException {
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

    public Trace trace() {
        if (contents instanceof Trace) {
            return (Trace) contents;
        }
        return null;
    }

    public TraceChain traceChain() {
        if (contents instanceof TraceChain) {
            return (TraceChain) contents;
        }
        return null;
    }

    public Message message() {
        if (contents instanceof Message) {
            return (Message) contents;
        }
        return null;
    }

    public CrashReport crashReport() {
        if (contents instanceof CrashReport) {
            return (CrashReport) contents;
        }
        return null;
    }
}
