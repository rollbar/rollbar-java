package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.InvalidLengthException;
import com.rollbar.payload.utilities.Validate;

import java.util.ArrayList;

public class TraceChain implements BodyContents {
    public static TraceChain fromThrowable(Throwable error) throws ArgumentNullException {
        Validate.isNotNull(error, "error");
        ArrayList<Trace> chain = new ArrayList<Trace>();
        do {
            chain.add(Trace.fromThrowable(error));
            error = error.getCause();
        } while(error != null);
        Trace[] traces = chain.toArray(new Trace[chain.size()]);
        try {
            return new TraceChain(traces);
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("non null errors should never result in zero length chains");
        }
    }

    private final Trace[] traces;

    public TraceChain(Trace[] traces) throws ArgumentNullException, InvalidLengthException {
        Validate.isNotNull(traces, "traces");
        Validate.minLength(traces, 1, "traces");
        this.traces = traces.clone();
    }

    public Trace[] traces() {
        return this.traces.clone();
    }

    public TraceChain traces(Trace[] traces) throws ArgumentNullException, InvalidLengthException {
        return new TraceChain(traces);
    }
}
