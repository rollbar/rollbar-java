package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.payload.data.body.BodyContents;

import java.lang.reflect.Type;
import java.util.HashMap;

public class BodyAdapter  implements JsonSerializer<BodyContents> {
    public JsonElement serialize(BodyContents contents, Type type, JsonSerializationContext jsonSerializationContext) {
        HashMap<String, Object> toSerialize = new HashMap<String, Object>();
        toSerialize.put(type.getTypeName(), contents);
        return jsonSerializationContext.serialize(toSerialize);
    }
}
