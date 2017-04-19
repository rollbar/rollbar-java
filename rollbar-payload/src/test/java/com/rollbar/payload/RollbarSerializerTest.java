package com.rollbar.payload;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.rollbar.payload.data.Data;
import com.rollbar.payload.data.Notifier;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.data.body.Message;
import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.InvalidLengthException;

import com.rollbar.utilities.RollbarSerializer;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class RollbarSerializerTest {
    private static final String accessToken = "e3a49f757f86465097c000cb2de9de08";
    private static final String environment = "testing";
    private static final String testMessage = "Test Serialize";
    private final static String basicExpected = "{\"access_token\":\"" + accessToken + "\",\"data\":{\"environment\":\"" + environment + "\",\"body\":{\"message\":{\"body\":\"" + testMessage + "\",\"extra\":\"has-extra\"}},\"notifier\":{\"name\":\"rollbar\"}}}";

    @Test
    public void TestBasicSerialize() {
        try {
            final LinkedHashMap<String, Object> members = new LinkedHashMap<String, Object>();
            members.put("extra", "has-extra");
            final Body body = Body.fromString(testMessage, members);
            final Data data = new Data(environment, body)
                    .notifier(new Notifier());

            String json = new RollbarSerializer(false).serialize(new Payload(accessToken, data));
            assertEquals(basicExpected, json);
        } catch (ArgumentNullException e) {
            fail("Message isn't null");
        } catch (InvalidLengthException e) {
            fail("Doesn't apply here");
        }
    }

    @Test
    public void TestExceptionSerialize() {
        try {
            final Body body = Body.fromError(getError());
            final Data data = new Data(environment, body);
            String json = new RollbarSerializer(true).serialize(new Payload(accessToken, data));
            JsonObject parsed = (JsonObject) new JsonParser().parse(json);
            assertEquals(accessToken, parsed.get("access_token").getAsString());
            assertEquals(environment, parsed.getAsJsonObject("data").get("environment").getAsString());
            assertEquals("Exception", parsed.getAsJsonObject("data").getAsJsonObject("body").getAsJsonObject("trace").getAsJsonObject("exception").get("class").getAsString());
            assertEquals("Non Chained Exception", parsed.getAsJsonObject("data").getAsJsonObject("body").getAsJsonObject("trace").getAsJsonObject("exception").get("message").getAsString());

            final JsonArray frames = parsed.getAsJsonObject("data").getAsJsonObject("body").getAsJsonObject("trace").getAsJsonArray("frames");
            final JsonObject lastFrame = frames.get(frames.size() - 1).getAsJsonObject();
            final JsonObject secondToLastFrame = frames.get(frames.size() - 2).getAsJsonObject();

            assertEquals("RollbarSerializerTest.java", lastFrame.get("filename").getAsString());
            assertEquals("RollbarSerializerTest.java", secondToLastFrame.get("filename").getAsString());
            assertEquals("com.rollbar.payload.RollbarSerializerTest", lastFrame.get("class_name").getAsString());
            assertEquals("com.rollbar.payload.RollbarSerializerTest", secondToLastFrame.get("class_name").getAsString());
            assertEquals("throwException", lastFrame.get("method").getAsString());
            assertEquals("getError", secondToLastFrame.get("method").getAsString());
        } catch (ArgumentNullException e) {
            fail("getError always returns an error");
        } catch (InvalidLengthException e) {
            fail("Doesn't apply here");
        }
    }

    @Test
    public void TestChainedExceptionSerialize() {
        try {
            final Body body = Body.fromError(getChainedError());
            final Data data = new Data(environment, body);
            String json = new RollbarSerializer().serialize(new Payload(accessToken, data));
            JsonObject parsed = (JsonObject) new JsonParser().parse(json);
            assertEquals(accessToken, parsed.get("access_token").getAsString());
            assertEquals(environment, parsed.getAsJsonObject("data").get("environment").getAsString());
            final JsonObject b = parsed.getAsJsonObject("data").getAsJsonObject("body");
            assertEquals(2, b.getAsJsonArray("trace_chain").size());
        } catch (ArgumentNullException e) {
            fail("getError always returns an error");
        } catch (InvalidLengthException e) {
            fail("Doesn't apply here");
        }
    }

    @Test
    public void TestExtensibleSerialize() {
        try {
            final Message msg = new Message("Message").put("extra", "value");
            final Body body = new Body(msg);
            final Data data = new Data(environment, body);
            String json = new RollbarSerializer(false).serialize(new Payload(accessToken, data));
            JsonObject parsed = (JsonObject) new JsonParser().parse(json);
            final String b = parsed.getAsJsonObject("data").getAsJsonObject("body").getAsJsonObject("message").get("extra").getAsString();
            assertEquals("value", b);
        } catch (ArgumentNullException e) {
            fail("Doesn't apply here");
        } catch (InvalidLengthException e) {
            fail("Doesn't apply here");
        }
    }

    public Throwable getError() {
        try {
            throwException();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private void throwException() throws Exception {
        throw new Exception("Non Chained Exception");
    }

    public Throwable getChainedError() {
        try {
            throwChainedError();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    public void throwChainedError() throws Exception {
        try {
            throwException();
        } catch (Exception e) {
            throw new Exception("Wrapper Exception", e);
        }
    }
}