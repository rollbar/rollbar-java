package com.rollbar.payload.data.body;

import com.rollbar.testing.GetAndSet;
import com.rollbar.testing.TestThat;
import com.rollbar.utilities.ArgumentNullException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by chris on 11/25/15.
 */
public class ExceptionInfoTest {
    private ExceptionInfo e;

    @Before
    public void setUp() {
        e = new ExceptionInfo("Test");
    }

    @Test
    public void testClassName() throws Exception {
        TestThat.getAndSetWorks(e, "BigException", "LittleException", new GetAndSet<ExceptionInfo, String>() {
            public String get(ExceptionInfo exceptionInfo) {
                return exceptionInfo.className();
            }

            public ExceptionInfo set(ExceptionInfo exceptionInfo, String val) {
                try {
                    return exceptionInfo.className(val);
                } catch (ArgumentNullException e1) {
                    fail("Neither option is null");
                    return null;
                }
            }
        });
    }

    @Test
    public void testMessage() throws Exception {
        TestThat.getAndSetWorks(e, "Hello World", "What a mess", new GetAndSet<ExceptionInfo, String>() {
            public String get(ExceptionInfo exceptionInfo) {
                return exceptionInfo.message();
            }

            public ExceptionInfo set(ExceptionInfo exceptionInfo, String val) {
                return exceptionInfo.message(val);
            }
        });
    }

    @Test
    public void testDescription() throws Exception {
        TestThat.getAndSetWorks(e, "A horrific exception", "Apocalypse begunß", new GetAndSet<ExceptionInfo, String>() {
            public String get(ExceptionInfo exceptionInfo) {
                return exceptionInfo.description();
            }

            public ExceptionInfo set(ExceptionInfo exceptionInfo, String val) {
                return exceptionInfo.description(val);
            }
        });
    }
}