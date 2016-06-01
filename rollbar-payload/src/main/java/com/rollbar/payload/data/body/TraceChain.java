package com.rollbar.payload.data.body;

import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.InvalidLengthException;
import com.rollbar.utilities.JsonSerializable;
import com.rollbar.utilities.Validate;

import java.util.ArrayList;

/**
 * Represents a chain of errors (typically from Exceptions with {@link Exception#getCause()} returning some value)
 */
public class TraceChain implements BodyContents, JsonSerializable {
    private final Trace[] traces;

    /**
     * Constructor
     * @param traces the traces making up this trace chain
     * @throws ArgumentNullException if traces are null
     * @throws InvalidLengthException if there are no traces in the array
     */
    public TraceChain(Trace[] traces) throws ArgumentNullException, InvalidLengthException {
        Validate.isNotNull(traces, "traces");
        Validate.minLength(traces, 1, "traces");
        this.traces = traces.clone();
    }

    /**
     * Generate a TraceChain from a throwable with multiple causes
     * @param error the error to record
     * @return the trace chain representing the Throwable
     * @throws ArgumentNullException if error is null
     */
    public static TraceChain fromThrowable(Throwable error) throws ArgumentNullException {
        return fromThrowable(error, null);
    }

    /**
     * Generate a TraceChain from a throwable with multiple causes
     * @param error the error to record
     * @param description a human readable description of the first error in the chain
     * @throws ArgumentNullException if error is null
     * @return the trace chain representing the Throwable
     */
    public static TraceChain fromThrowable(Throwable error, String description) throws ArgumentNullException {
        Validate.isNotNull(error, "error");
        ArrayList<Trace> chain = new ArrayList<Trace>();
        do {
            chain.add(Trace.fromThrowable(error, description));
            description = null;
            error = error.getCause();
        } while(error != null);
        Trace[] traces = chain.toArray(new Trace[chain.size()]);
        return new TraceChain(traces);
    }

    /**
     * @return a copy of the trace array
     */
    public Trace[] traces() {
        return this.traces.clone();
    }

    /**
     * Set the trace array on a copy of this trace chain
     * @param traces the traces to set
     * @return a new TraceChain with the traces given
     * @throws ArgumentNullException if traces are null
     * @throws InvalidLengthException if there are no traces in the array
     */
    public TraceChain traces(Trace[] traces) throws ArgumentNullException, InvalidLengthException {
        return new TraceChain(traces);
    }

    public Trace[] asJson() {
        return traces();
    }
}
