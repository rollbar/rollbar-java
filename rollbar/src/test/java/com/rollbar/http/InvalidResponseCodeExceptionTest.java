package com.rollbar.http;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by chris on 11/25/15.
 */
public class InvalidResponseCodeExceptionTest {
    @Test
    public void testValue() throws Exception {
        InvalidResponseCodeException e = new InvalidResponseCodeException(12);
        assertEquals(12, e.value());
        assertTrue(e.getMessage().contains("12"));
    }
}