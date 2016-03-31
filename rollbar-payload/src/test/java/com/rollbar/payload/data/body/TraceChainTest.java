package com.rollbar.payload.data.body;

import com.rollbar.testing.*;

import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.InvalidLengthException;

import org.junit.Before;
import org.junit.Test;

import java.net.ConnectException;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by chris on 11/25/15.
 */
public class TraceChainTest {

    private TraceChain t;

    @Before
    public void setUp() throws Exception {
        t = TraceChain.fromThrowable(getChainedError());
    }

    @Test
    public void testTraces() throws Exception {
        Trace one = Trace.fromThrowable(new ArgumentNullException("null_argument"));
        Trace two = Trace.fromThrowable(new ConnectException("oops"));
        TestThat.getAndSetWorks(t, new Trace[] { one }, new Trace[] { two }, new GetAndSet<TraceChain, Trace[]>() {
            public Trace[] get(TraceChain traceChain) {
                return traceChain.traces();
            }

            public TraceChain set(TraceChain traceChain, Trace[] val) {
                try {
                    return traceChain.traces(val);
                } catch (ArgumentNullException e) {
                    fail("nothing's null");
                } catch (InvalidLengthException e) {
                    fail("Everything's at least len 1");
                }
                return null;
            }
        });
    }

    private void causeError() {
        throw new RuntimeException("TRICKY!");
    }

    private Throwable getError() {
        try {
            causeError();
        } catch (Throwable t) {
            return t;
        }
        return null;
    }

    private void causeChainedError() {
        try {
            causeError();
        } catch (Throwable t) {
            throw new IllegalStateException("Nested Tricky!", t);
        }
    }

    private Throwable getChainedError() {
        try {
            causeChainedError();
        } catch (Throwable t) {
            return t;
        }
        return null;
    }
}