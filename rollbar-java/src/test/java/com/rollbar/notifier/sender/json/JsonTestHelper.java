package com.rollbar.notifier.sender.json;

import com.google.gson.Gson;

import java.util.Map;

public class JsonTestHelper {
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
