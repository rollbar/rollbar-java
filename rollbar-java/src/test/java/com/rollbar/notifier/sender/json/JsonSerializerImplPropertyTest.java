package com.rollbar.notifier.sender.json;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.ExceptionInfo;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Trace;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

import static com.rollbar.notifier.sender.json.JsonTestHelper.getValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;

/**
 * A "fuzzing-like" approach to test our hand-rolled serializer. Populates a payload object with
 * different types of problematic strings, and ensures it produces correct json every time.
 */
@RunWith(Parameterized.class)
public class JsonSerializerImplPropertyTest {
    private final String badString;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[][] params() {
        List<String> parameters = new ArrayList<>(Collections.singletonList("\u0008"));

        IntStream.range(0, 127).mapToObj(c -> Character.valueOf((char) c).toString())
                .forEach(parameters::add);

        int emojiStart = 0x1F600;
        int emojiEnd = emojiStart + 50;

        IntStream.range(emojiStart, emojiEnd).mapToObj(c -> Character.valueOf((char) c).toString())
                .forEach(parameters::add);

        parameters.add("NotificationRequest(topicName=DOCUMENT_STATUS_CHANGED, message={\"id\":738899,\"internalDocumentId\":\"ea15b4a5-23ba-40fa-bcb8-d97216a26478\",\"event\":\"DOCUMENT_STATUS_CHANGED\",\"eventTime\":\"2021-04-21T08:42:44.810Z\",\"details\":{\"type\":\"DOCUMENT_STATUS_CHANGED\",\"oldStatus\":\"PENDING\",\"newStatus\":\"UNRESOLVED\"}}, stringAttributes={docid-ea15b4a5-23ba-40fa-bcb8-d97216a26478=ea15b4a5-23ba-40fa-bcb8-d97216a26478})");
        parameters.add("{ \"test\": { \"a\": 3 }");
        parameters.add("{ \"test\": { \"a\": \"how }");
        parameters.add("{ \"test\": { \"a\": \"how } }");
        parameters.add("{ \"test': { \"a\": 3 }");
        parameters.add("{ 'test\": { \"a\": \"how }");
        parameters.add("{ \"test\": { 'a\": \"how } }");

        parameters.add("\"{docid-ea15b4a5-23ba-40fa-bcb8-d97216a26478=ea15b4a5-23ba-40fa-bcb8-d97216a26478}\"");

        return parameters.stream().map(v -> new Object[] { v }).toArray(Object[][]::new);
    }

    public JsonSerializerImplPropertyTest(String badString) {
        this.badString = badString;
    }

    @Test
    public void shouldSerializeAnyPayloadValue() {
        String exceptionMessage = "exception: " + badString;
        testRecursiveJson(badString, badString, exceptionMessage, 2);
    }

    private void testRecursiveJson(String key, String value, String message, int level) {
        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put(key, value);
        addMoreCustom(extraInfo, key, value);

        ExceptionInfo exception = new ExceptionInfo.Builder()
                .className("UnsupportedIdeaException")
                .message(message)
                .build();

        ArrayList<Frame> frames = new ArrayList<>();

        frames.add(new Frame.Builder()
                .code(message)
                .className(key)
                .filename(value).build());

        frames.add(new Frame.Builder()
                .code("]")
                .className("[")
                .filename("}").build());

        Trace trace = new Trace.Builder()
                .exception(exception)
                .frames(frames)
                .build();

        Data data = new Data.Builder()
                .body(new Body.Builder().bodyContent(trace).build())
                .custom(extraInfo)
                .build();

        Payload payload = new Payload.Builder()
                .accessToken("ignored")
                .data(data)
                .build();

        JsonSerializerImpl serializer = new JsonSerializerImpl();

        String serializedData = serializer.toJson(payload);

        Map<String, Object> recovered = JsonTestHelper.fromString(serializedData);
        assertNotNull(recovered);

        Map<String, Object> dataMap = getValue(recovered, "data");
        String payloadMessage = getValue(dataMap, "body", "trace", "exception", "message");

        assertThat(payloadMessage, equalTo(message));

        String custom = getValue(dataMap, "custom", key);
        assertThat(custom, equalTo(value));

        ArrayList<String> values = getValue(dataMap, "custom", key + "array");
        assertThat(values, hasSize(5));
        assertThat(values.get(0), equalTo(value));
        assertThat(values.get(1), equalTo("SerializesBadly(message=\"quoted string\")"));
        assertThat(values.get(2), startsWith("SerializesBadly(message="));
        assertThat(values.get(3), equalTo("BadThrowable(message=\"quoted string\")"));
        assertThat(values.get(4), startsWith("BadThrowable(message="));

        List<Map<String, Object>> payloadFrames = getValue(dataMap, "body", "trace", "frames");
        assertThat(payloadFrames, hasSize(2));

        String code = getValue(payloadFrames.get(0), "code");
        assertThat(code, equalTo(message));

        String className = getValue(payloadFrames.get(0), "class_name");
        assertThat(className, equalTo(key));

        String filename = getValue(payloadFrames.get(0), "filename");
        assertThat(filename, equalTo(value));

        if (level > 0) {
            // Now use the entire json string as the key and value for the next test
            testRecursiveJson(key, serializedData, serializedData, level - 1);
        }
    }

    private void addMoreCustom(Map<String, Object> extraInfo, String infoKey, String infoValue) {
        extraInfo.put(infoKey + "array", new Object[]{
                infoValue,
                new SerializesBadly(),
                new SerializesBadly(infoValue),
                new BadThrowable(),
                new BadThrowable(infoValue)
        });
    }

    static class SerializesBadly {
        private final String message;

        public SerializesBadly() {
            this("\"quoted string\"");
        }

        public SerializesBadly(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "SerializesBadly(" +
                    "message=" + message + ')';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SerializesBadly that = (SerializesBadly) o;
            return Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }

    static class BadThrowable extends Throwable {
        private final String message;

        public BadThrowable() {
            this("\"quoted string\"");
        }

        public BadThrowable(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "BadThrowable(" +
                    "message=" + message + ')';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BadThrowable that = (BadThrowable) o;
            return Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }
}
