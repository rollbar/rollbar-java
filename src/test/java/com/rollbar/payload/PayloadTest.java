package com.rollbar.payload;

import com.rollbar.payload.data.Data;
import com.rollbar.payload.data.body.Body;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PayloadTest {
    private Body b;
    private Data d;
    private Payload p;

    @Before
    public void setUp() throws Exception {
        b = Body.fromString("Hello");
        d = new Data("ENVIRONMENT", b);
        p = new Payload("TOKEN", d);
    }

    @Test
    public void testAccessTokenGet() throws Exception {
        assertEquals("TOKEN", p.accessToken());
    }

    @Test
    public void testDataGet() throws Exception {
        assertSame(p.data(), d);
    }

    @Test
    public void testAccessTokenSet() throws Exception {
        Payload withNewToken = p.accessToken("New Token");
        assertEquals("TOKEN", p.accessToken());
        assertEquals(withNewToken.accessToken(), "New Token");
        assertNotSame(p, withNewToken);
        assertSame(p.data(), withNewToken.data());
    }

    @Test
    public void testDataSet() throws Exception {
        Payload withNewData = p.data(new Data("OTHER ENVIRONMENT", b));
        assertEquals("ENVIRONMENT", p.data().environment());
        assertEquals("OTHER ENVIRONMENT", withNewData.data().environment());
        assertNotSame(p, withNewData);
        assertSame(p.accessToken(), withNewData.accessToken());
    }
}