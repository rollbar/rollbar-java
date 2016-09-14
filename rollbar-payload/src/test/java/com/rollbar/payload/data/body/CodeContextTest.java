package com.rollbar.payload.data.body;

import com.rollbar.testing.GetAndSet;
import com.rollbar.testing.TestThat;
import org.junit.Test;

/**
 * Created by chris on 11/25/15.
 */
public class CodeContextTest {
    private final CodeContext c = new CodeContext();

    @Test
    public void testPre() throws Exception {
        String[] one = new String [] { "// 42" };
        String[] two = new String[] { "Integer i = x;" };
        TestThat.getAndSetWorks(c, one, two, new GetAndSet<CodeContext, String[]>() {
            public String[] get(CodeContext codeContext) {
                return codeContext.pre();
            }

            public CodeContext set(CodeContext codeContext, String[] val) {
                return codeContext.pre(val);
            }
        });
    }

    @Test
    public void testPost() throws Exception {
        String[] one = new String[] { "    } ", "    " };
        String[] two = new String[] { " int a = b; ", " b = c; ", " return c = a; " };
        TestThat.getAndSetWorks(c, one, two, new GetAndSet<CodeContext, String[]>() {
            public String[] get(CodeContext codeContext) {
                return codeContext.post();
            }

            public CodeContext set(CodeContext codeContext, String[] val) {
                return codeContext.post(val);
            }
        });
    }
}