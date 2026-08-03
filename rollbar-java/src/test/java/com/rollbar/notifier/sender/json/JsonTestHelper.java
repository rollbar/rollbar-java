package com.rollbar.notifier.sender.json;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class JsonTestHelper {

    /**
     * Strictly parses the whole document, failing on anything Gson's lenient default
     * would wave through. {@link #fromString} is lenient, so it cannot by itself prove
     * the serializer emitted well formed JSON - an unterminated string, for instance,
     * can survive a lenient parse.
     *
     * @param serializedData the JSON to validate.
     */
    public static void assertValidJson(String serializedData) {
        JsonReader reader = new JsonReader(new StringReader(serializedData));
        reader.setLenient(false);
        try {
            skipValue(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new AssertionError("Trailing content after JSON document: " + serializedData);
            }
        } catch (IOException | RuntimeException e) {
            throw new AssertionError("Not valid JSON: " + serializedData, e);
        }
    }

    private static void skipValue(JsonReader reader) throws IOException {
        // skipValue() itself is lenient about structure, so walk the document instead.
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                reader.beginObject();
                while (reader.hasNext()) {
                    reader.nextName();
                    skipValue(reader);
                }
                reader.endObject();
                break;
            case BEGIN_ARRAY:
                reader.beginArray();
                while (reader.hasNext()) {
                    skipValue(reader);
                }
                reader.endArray();
                break;
            case STRING:
                reader.nextString();
                break;
            case NUMBER:
                reader.nextDouble();
                break;
            case BOOLEAN:
                reader.nextBoolean();
                break;
            case NULL:
                reader.nextNull();
                break;
            default:
                throw new AssertionError("Unexpected token: " + reader.peek());
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromString(String serializedData) {
        // Gson's Json compliance seems to be pretty good, let's see if it can deserialize our
        // payload
        Gson gson = new Gson();
        return gson.fromJson(serializedData, Map.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValue(Map<String, Object> source, String attribute,
                                 String... attributes) {
        Object value = source.get(attribute);

        if (attributes.length == 0) {
            return (T) value;
        }

        if (value == null) {
            throw new NullPointerException("No value with key " + attribute);
        }

        Map<String, Object> asMap = (Map<String, Object>)value;
        String[] newAttributes = new String[attributes.length - 1];
        System.arraycopy(attributes, 1, newAttributes, 0, newAttributes.length);

        return getValue(asMap, attributes[0], newAttributes);
    }

}
