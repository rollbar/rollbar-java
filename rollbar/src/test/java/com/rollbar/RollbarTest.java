package com.rollbar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RollbarTest {

    @Test
    public void itDoesNotThrowAnExceptionWhenConfiguringWithJustAccessTokenAndEnvironment() {
        Rollbar rollbar = new Rollbar("some-access-token", "some-environment");
        assertEquals("some-access-token", rollbar.getAccessToken());
        assertEquals("some-environment", rollbar.getEnvironment());
    }
}
