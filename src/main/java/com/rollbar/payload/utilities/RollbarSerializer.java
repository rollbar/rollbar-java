package com.rollbar.payload.utilities;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rollbar.payload.Payload;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.data.body.TraceChain;

/**
 * Created by chris on 11/17/15.
 */
public class RollbarSerializer implements PayloadSerializer {
    private final static GsonBuilder builder = new GsonBuilder()
            .registerTypeHierarchyAdapter(Extensible.class, new ExtensibleAdapter())
            .registerTypeAdapter(Body.class, new BodyAdapter())
            .registerTypeAdapter(TraceChain.class, new TraceChainAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

    private final Gson gson;

    public RollbarSerializer() {
        gson = builder.create();
    }

    public RollbarSerializer(boolean prettyPrint) {
        if (prettyPrint) {
            gson = builder.setPrettyPrinting().create();
        } else {
            gson = builder.create();
        }
    }

    public String serialize(Payload payload) {
        return gson.toJson(payload);
    }
}
