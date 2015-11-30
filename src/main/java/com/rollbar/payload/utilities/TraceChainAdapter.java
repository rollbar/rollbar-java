package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.payload.data.body.TraceChain;

import java.lang.reflect.Type;

/**
 * The Adapter for Trace Chains. Serializes the whole thing as an array.
 */
public class TraceChainAdapter implements JsonSerializer<TraceChain> {
    /**
     * Serializes a TraceChain as an array of traces.
     * @param traceChain the trace chain
     * @param type the runtime type of the trace chain
     * @param jsonSerializationContext the context doing the serializing
     * @return the JsonElement representing the TraceChain
     */
    public JsonElement serialize(TraceChain traceChain, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(traceChain.traces());
    }
}
