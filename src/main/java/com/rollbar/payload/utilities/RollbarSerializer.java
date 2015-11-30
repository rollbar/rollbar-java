package com.rollbar.payload.utilities;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rollbar.payload.Payload;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.data.body.TraceChain;

/**
 * A Payload Serializer
 */
public class RollbarSerializer implements PayloadSerializer {
    private final static GsonBuilder builder = new GsonBuilder()
            .registerTypeHierarchyAdapter(Extensible.class, new ExtensibleAdapter())
            .registerTypeAdapter(Body.class, new BodyAdapter())
            .registerTypeAdapter(TraceChain.class, new TraceChainAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

    private final Gson gson;

    /**
     * Construct a RollbarSerializer that does <b>not</b> pretty print the Payload
     */
    public RollbarSerializer() {
        this(false);
    }

    /**
     * Construct a RollbarSerializer.
     * @param prettyPrint whether or not to pretty print the payload.
     */
    public RollbarSerializer(boolean prettyPrint) {
        if (prettyPrint) {
            gson = builder.setPrettyPrinting().create();
        } else {
            gson = builder.create();
        }
    }

    /**
     * @param payload the {@link Payload} to serialize
     * @return the json representation of the payload
     */
    public String serialize(Payload payload) {
        return gson.toJson(payload);
    }
}
