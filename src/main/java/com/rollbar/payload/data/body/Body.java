package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.JsonSerializable;
import com.rollbar.payload.utilities.Validate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A container for the actual error(s), message, or crash report that caused this error.
 */
public class Body implements JsonSerializable {
    /**
     * Create a Body from an error. If {@link Throwable#getCause()} isn't null will return a Trace Chain,
     * otherwise returns a Trace
     * @param error the error to turn into a `Body`
     * @return the Rollbar Body constructed from the error
     * @throws ArgumentNullException if error is null
     */
    public static Body fromError(Throwable error) throws ArgumentNullException {
        return fromError(error, null);
    }

    /**
     * Create a Body from an error with a human readable descriptpion. If {@link Throwable#getCause()} isn't null will
     * return a Trace Chain
     * @param error the error to turn into a Body
     * @param description the human readable description of the top level error in the chain (or the error itself if not
     *                    a chained error).
     * @return the Rollbar Body constructed from the error
     * @throws ArgumentNullException if error is null
     */
    public static Body fromError(Throwable error, String description) throws ArgumentNullException {
        Validate.isNotNull(error, "error");
        if (error.getCause() == null) {
            return Body.trace(error, description);
        } else {
            return Body.traceChain(error, description);
        }
    }

    private static Body traceChain(Throwable error, String description) throws ArgumentNullException {
        final TraceChain chain = TraceChain.fromThrowable(error, description);
        return new Body(chain);
    }

    private static Body trace(Throwable error, String description) throws ArgumentNullException {
        final Trace trace = Trace.fromThrowable(error, description);
        return new Body(trace);
    }

    /**
     * Create a Body from a string message.
     * @param message the message to convert into a Rollbar Message
     * @return a body containing a message containing this message body
     * @throws ArgumentNullException if message is null or whitespace
     */
    public static Body fromString(String message) throws ArgumentNullException {
        return Body.fromString(message, null);
    }

    /**
     * Create a Body from a string message and additional arguments
     * @param message the message to convert into a Rollbar Message
     * @param extra the extra data to send to Rollbar
     * @return a body containing a message containing this message body and extra arguments
     * @throws ArgumentNullException if message is null or whitespace
     */
    public static Body fromString(String message, Map<String, Object> extra) throws ArgumentNullException {
        final BodyContents contents = new Message(message, extra);
        return new Body(contents);
    }

    /**
     * Create a crash report body from a string
     * @param raw the crash report content
     * @return a body made from the crash report
     * @throws ArgumentNullException if raw is null
     */
    public static Body fromCrashReportString(String raw) throws ArgumentNullException {
        final CrashReport contents = new CrashReport(raw);
        return new Body(contents);
    }

    private final BodyContents contents;

    /**
     * Constructor
     * @param contents the contents of this body (either Trace, TraceChain, Message, or CrashReport)
     * @throws ArgumentNullException if contents is nul
     */
    public Body(BodyContents contents) throws ArgumentNullException {
        Validate.isNotNull(contents, "contents");
        this.contents = contents;
    }

    /**
     * @return the contents
     */
    public BodyContents contents() {
        return contents;
    }

    /**
     * Set the contents in a copy of this Body
     * @param contents the contents
     * @return a copy of this Body with the new contents
     * @throws ArgumentNullException if contents is null
     */
    public Body contents(BodyContents contents) throws ArgumentNullException {
        return new Body(contents);
    }

    /**
     * Get the contents as a Trace, returns null if the contents is *not* a Trace
     * @return the contents as a Trace
     */
    public Trace trace() {
        if (contents instanceof Trace) {
            return (Trace) contents;
        }
        return null;
    }

    /**
     * Get the contents as a TraceChain, returns null if the contents is *not* a TraceChain
     * @return the contents as a TraceChain
     */
    public TraceChain traceChain() {
        if (contents instanceof TraceChain) {
            return (TraceChain) contents;
        }
        return null;
    }

    /**
     * Get the contents as a Message, returns null if the contents is *not* a Message
     * @return the contents as a Message
     */
    public Message message() {
        if (contents instanceof Message) {
            return (Message) contents;
        }
        return null;
    }

    /**
     * Get the contents as a CrashReport, returns null if the contents is *not* a CrashReport
     * @return the contents as a CrashReport
     */
    public CrashReport crashReport() {
        if (contents instanceof CrashReport) {
            return (CrashReport) contents;
        }
        return null;
    }

    public Map<String, Object> asJson() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put(key(), contents());
        return obj;
    }

    private String key() {
        return toSnakeCase(contents.getClass().getSimpleName());
    }

    private static String toSnakeCase(String simpleName) {
        return String.join("_", simpleName.split("(?=\\p{Lu})")).toLowerCase();
    }
}
