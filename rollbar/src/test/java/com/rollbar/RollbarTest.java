package com.rollbar;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class RollbarTest {

    @Test
    public void itDoesNotThrowAnExceptionWhenConfiguringWithJustAccessTokenAndEnvironment() {
        Boolean exceptionDidHappen = false;

        try {
            new Rollbar("some-access-token", "some-environment");
        } catch (NullPointerException e) {
            exceptionDidHappen = true;
        }

        assertFalse(exceptionDidHappen);
    }
}
