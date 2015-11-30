package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.payload.data.body.Body;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * A JsonSerializer that serializes the {@link Body}.
 */
public class BodyAdapter  implements JsonSerializer<Body> {
    /**
     * Serializes the body for GSON (names the `content` with snake_case, based on its class name).
     * @param body the body to serialize
     * @param type the type being serialized
     * @param jsonSerializationContext the context doing the serializing
     * @return
     */
    public JsonElement serialize(Body body, Type type, JsonSerializationContext jsonSerializationContext) {
        HashMap<String, Object> toSerialize = new HashMap<String, Object>();
        toSerialize.put(toSnakeCase(body.contents().getClass().getSimpleName()), body.contents());
        return jsonSerializationContext.serialize(toSerialize);
    }

    private static String toSnakeCase(String simpleName) {
        return String.join("_", simpleName.split("(?=\\p{Lu})")).toLowerCase();
    }
}
