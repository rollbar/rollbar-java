package com.rollbar.http;

import com.rollbar.payload.utilities.ArgumentNullException;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import static org.junit.Assert.*;

public class PayloadSenderTest {
    private final PayloadSender sender;

    public PayloadSenderTest() throws MalformedURLException, ArgumentNullException {
        try {
            sender = new PayloadSender();
        } catch (ArgumentNullException e) {
            fail("This shouldn't happen since I didn't pass null to PayloadSender");
            throw e;
        } catch (MalformedURLException e) {
            fail("This shouldn't happen since I passed a valid URL to PayloadSender");
            throw e;
        }
    }
    @Test
    public void SendInvalidWorks() {
        try {
            RollbarResponse response = sender.Send("{}");
            assertEquals(RollbarResponseCode.BadRequest, response.statusCode());
            assertNotNull(response.errorMessage());
            assertNull(response.uuid());
        } catch (ConnectionFailedException e) {
            fail("This shouldn't happen unless you're not connected to the internet.");
        } catch (UnsupportedEncodingException e) {
            fail("UTF-8 should support '{}'");
        }
    }

    @Test
    public void SendValidWorks() {
        try {
            RollbarResponse response = sender.Send("{\n" +
                    "  \"access_token\": \"e3a49f757f86465097c000cb2de9de08\",\n" +
                    "  \"data\": {\n" +
                    "    \"environment\": \"production\",\n" +
                    "    \"body\": {\n" +
                    "      \"message\": {\n" +
                    "        \"body\": \"Hello, world!\"\n" +
                    "      },\n" +
                    "     \"notifier\": {\n" +
                    "        \"name\": \"java-rollbar\",\n" +
                    "        \"version\": \"alpha-0.1\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}");
            assertEquals(RollbarResponseCode.Success, response.statusCode());
            assertNotNull(response.uuid());
            assertNull(response.errorMessage());
        } catch (ConnectionFailedException e) {
            fail("This shouldn't happen unless you're not connected to the internet.");
        } catch (UnsupportedEncodingException e) {
            fail("UTF-8 should support the payload");
        }
    }
}