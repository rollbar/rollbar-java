package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.payload.data.body.TraceChain;

import java.lang.reflect.Type;

/**
 * Created by chris on 11/17/15.
 */
public class TraceChainAdapter implements JsonSerializer<TraceChain> {
    public JsonElement serialize(TraceChain traceChain, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(traceChain.traces());
    }
}
