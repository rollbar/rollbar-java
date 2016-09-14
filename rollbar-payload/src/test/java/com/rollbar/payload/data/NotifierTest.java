package com.rollbar.payload.data;

import com.rollbar.testing.GetAndSet;
import com.rollbar.testing.TestThat;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by chris on 11/20/15.
 */
public class NotifierTest {

    private Notifier notifier;

    @Before
    public void setUp() {
        notifier = new Notifier("RollbarJava", "alpha");
    }

    @Test
    public void testName() throws Exception {
        TestThat.getAndSetWorks(notifier, "name-one", "name-two", new GetAndSet<Notifier, String>() {
            public String get(Notifier item) {
                return item.name();
            }

            public Notifier set(Notifier item, String val) {
                return item.name(val);
            }
        });
    }

    @Test
    public void testVersion() throws Exception {
        TestThat.getAndSetWorks(notifier, "ecba12", "SHA-15f0a", new GetAndSet<Notifier, String>() {
            public String get(Notifier item) {
                return item.version();
            }

            public Notifier set(Notifier item, String val) {
                return item.version(val);
            }
        });
    }
}