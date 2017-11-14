package com.rollbar.notifier.util.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.api.payload.Payload;
import java.lang.reflect.Type;

public class PayloadSerializer implements JsonSerializer<Payload> {

  @Override
  public JsonElement serialize(Payload src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.add("access_token", context.serialize(src.getAccessToken()));
    jsonObject.add("data", context.serialize(src.getData()));

    return jsonObject;
  }
}
