package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.payload.data.body.Body;
import sun.misc.Regexp;

import java.lang.reflect.Type;
import java.util.HashMap;

public class BodyAdapter  implements JsonSerializer<Body> {
    public JsonElement serialize(Body body, Type type, JsonSerializationContext jsonSerializationContext) {
        HashMap<String, Object> toSerialize = new HashMap<String, Object>();
        toSerialize.put(toSnakeCase(body.contents().getClass().getSimpleName()), body.contents());
        return jsonSerializationContext.serialize(toSerialize);
    }

    public String toSnakeCase(String simpleName) {
        return String.join("_", simpleName.split("(?=\\p{Lu})")).toLowerCase();
    }
}
