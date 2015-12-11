package com.rollbar.payload.data.body;

import com.rollbar.GetAndSet;
import com.rollbar.TestThat;
import com.rollbar.payload.utilities.ArgumentNullException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Created by chris on 11/25/15.
 */
public class TraceTest {
    Trace t;

    @Before
    public void setUp() throws Exception {
        t = Trace.fromThrowable(new Exception("Urgh"));
    }

    private Throwable getISE() {
        try {
            throw new IllegalStateException("Oops");
        } catch (IllegalStateException e) {
            return e;
        }
    }

    private Throwable getException() {
        try {
            throw new Exception("Oy");
        } catch (Exception e) {
            return e;
        }
    }

    @Test
    public void testFrames() throws Exception {
        Frame[] one = Frame.fromThrowable(getISE());
        Frame[] two = Frame.fromThrowable(getException());

        TestThat.getAndSetWorks(t, one, two, new GetAndSet<Trace, Frame[]>() {
            public Frame[] get(Trace trace) {
                return trace.frames();
            }

            public Trace set(Trace trace, Frame[] val) {
                try {
                    return trace.frames(val);
                } catch (ArgumentNullException e) {
                    fail("Nothing's null");
                }
                return null;
            }
        });
    }

    @Test
    public void testException() throws Exception {
        ExceptionInfo one = ExceptionInfo.fromThrowable(getISE());
        ExceptionInfo two = ExceptionInfo.fromThrowable(getException());
        TestThat.getAndSetWorks(t, one, two, new GetAndSet<Trace, ExceptionInfo>() {
            public ExceptionInfo get(Trace trace) {
                return trace.exception();
            }

            public Trace set(Trace trace, ExceptionInfo val) {
                try {
                    return trace.exception(val);
                } catch (ArgumentNullException e) {
                    fail("Nothing's null");
                }
                return null;
            }
        });
    }
}