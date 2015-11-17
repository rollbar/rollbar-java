package com.rollbar.http;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rollbar.payload.Payload;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.utilities.BodyAdapter;
import com.rollbar.payload.utilities.Extensible;
import com.rollbar.payload.utilities.ExtensibleAdapter;

/**
 * Created by chris on 11/17/15.
 */
public class RollbarSerializer implements PayloadSerializer {
    private final static Gson gson = new GsonBuilder()
            .registerTypeAdapter(Extensible.class, new ExtensibleAdapter())
            .registerTypeAdapter(Body.class, new BodyAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public String serialize(Payload payload) {
        return gson.toJson(payload);
    }
}
