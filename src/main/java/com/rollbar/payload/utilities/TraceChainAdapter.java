package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.payload.data.body.TraceChain;

import java.lang.reflect.Type;

public class TraceChainAdapter implements JsonSerializer<TraceChain> {
    public JsonElement serialize(TraceChain traceChain, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(traceChain.traces());
    }
}
