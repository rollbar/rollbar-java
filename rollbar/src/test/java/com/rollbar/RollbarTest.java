package com.rollbar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RollbarTest {

    @Test
    public void itDoesNotThrowAnExceptionWhenConfiguringWithJustAccessTokenAndEnvironment() {
        Rollbar rollbar = new Rollbar("some-access-token", "some-environment");
        assertEquals("some-access-token", rollbar.getAccessToken());
        assertEquals("some-environment", rollbar.getEnvironment());
    }

    @Test
    public void itDoesNotThrowANullPointerExceptionWhenLoggingAnException() {
        Rollbar rollbar = new Rollbar("some-access-token", "some-environment");
        rollbar.log(new Exception("some exception"));
    }

    @Test
    public void sendItemToRollbar() {
        Rollbar rollbar = new Rollbar("e3a49f757f86465097c000cb2de9de08", "testing");
        rollbar.error(new Exception("this is a test exception")); // should not throw an exception
    }
}
