package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Specifies how to serialize Extensible subclasses.
 */
public class ExtensibleAdapter implements JsonSerializer<Extensible> {
    /**
     * Serialize an Extensible
     * @param extensible the extensible to serialize
     * @param type the Runtime type being serialized
     * @param jsonSerializationContext the jsonSerializationContext doing the serialization
     * @return the JsonElement produced
     */
    public JsonElement serialize(Extensible extensible, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(extensible.getMembers());
    }
}
