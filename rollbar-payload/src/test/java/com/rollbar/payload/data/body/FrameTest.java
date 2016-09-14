package com.rollbar.payload.data.body;

import com.rollbar.testing.GetAndSet;
import com.rollbar.testing.TestThat;
import com.rollbar.utilities.ArgumentNullException;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by chris on 11/25/15.
 */
public class FrameTest {
    private Frame f;

    @Before
    public void setUp() {
        f = new Frame("com.rollbar.payload.data.Frame.java");
    }

    @Test
    public void testFilename() throws Exception {
        TestThat.getAndSetWorks(f, "newfile.java", "{redacted}.java", new GetAndSet<Frame, String>() {
            public String get(Frame frame) {
                return frame.filename();
            }

            public Frame set(Frame frame, String val) {
                try {
                    return frame.filename(val);
                } catch (ArgumentNullException e) {
                    fail("Neither is null");
                    return null;
                }
            }
        });
    }

    @Test
    public void testLineNumber() throws Exception {
        TestThat.getAndSetWorks(f, 15, 20, new GetAndSet<Frame, Integer>() {
            public Integer get(Frame frame) {
                return frame.lineNumber();
            }

            public Frame set(Frame frame, Integer val) {
                return frame.lineNumber(val);
            }
        });
    }

    @Test
    public void testColumnNumber() throws Exception {
        TestThat.getAndSetWorks(f, 15, 20, new GetAndSet<Frame, Integer>() {
            public Integer get(Frame frame) {
                return frame.columnNumber();
            }

            public Frame set(Frame frame, Integer val) {
                return frame.columnNumber(val);
            }
        });
    }

    @Test
    public void testMethod() throws Exception {
        TestThat.getAndSetWorks(f, "method()", "method(String whatever)", new GetAndSet<Frame, String>() {
            public String get(Frame frame) {
                return frame.method();
            }

            public Frame set(Frame frame, String val) {
                return frame.method(val);
            }
        });
    }

    @Test
    public void testCode() throws Exception {
        TestThat.getAndSetWorks(f, "throw new CodeError()", "throw new Exception(\"Oops\")", new GetAndSet<Frame, String>() {
            public String get(Frame frame) {
                return frame.code();
            }

            public Frame set(Frame frame, String val) {
                return frame.code(val);
            }
        });
    }

    @Test
    public void testContext() throws Exception {
        CodeContext one = new CodeContext(new String[] { "before", "the", "code" }, new String[] { "after", "the", "code" });
        CodeContext two = new CodeContext(new String[] { "przed", "kodem" }, new String[] { "po", "kodu" });
        TestThat.getAndSetWorks(f, one, two, new GetAndSet<Frame, CodeContext>() {
            public CodeContext get(Frame frame) {
                return frame.context();
            }

            public Frame set(Frame frame, CodeContext val) {
                return frame.context(val);
            }
        });
    }

    @Test
    public void testArgs() throws Exception {
        Object[] one = new Object[] { 1, "Hello" };
        Object[] two = new Object[] { new String[] { "Hello", "World" }, null, 15 };
        TestThat.getAndSetWorks(f, one, two, new GetAndSet<Frame, Object[]>() {
            public Object[] get(Frame frame) {
                return frame.args();
            }

            public Frame set(Frame frame, Object[] val) {
                return frame.args(val);
            }
        });
    }

    @Test
    public void testKeywordArgs() throws Exception {
        LinkedHashMap<String, Object> one = new LinkedHashMap<String, Object>();
        one.put("Hello", "World");
        LinkedHashMap<String, Object> two = new LinkedHashMap<String, Object>();
        two.put("arr", new String[] { "Ugh", "What", "a", "mess" });
        two.put("val", 15);
        TestThat.getAndSetWorks(f, one, two, new GetAndSet<Frame, Map<String, Object>>() {
            public Map<String, Object> get(Frame frame) {
                return frame.keywordArgs();
            }

            public Frame set(Frame frame, Map<String, Object> val) {
                return frame.keywordArgs(val);
            }
        });
    }

    @Test
    public void testEmptyValues() {
        Frame frame = new Frame("foo.java");
        Map<String, Object> json = frame.asJson();
        assertFalse(json.containsKey("lineno"));
        assertFalse(json.containsKey("colno"));
        assertFalse(json.containsKey("method"));
        assertFalse(json.containsKey("code"));
        assertFalse(json.containsKey("context"));
        assertFalse(json.containsKey("args"));
        assertFalse(json.containsKey("kwargs"));
    }
}