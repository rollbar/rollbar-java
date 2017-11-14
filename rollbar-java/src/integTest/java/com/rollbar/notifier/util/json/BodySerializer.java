package com.rollbar.notifier.util.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Message;
import java.lang.reflect.Type;

public class BodySerializer implements JsonSerializer<Body> {

  @Override
  public JsonElement serialize(Body src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    if(src.getContents() instanceof Message) {
      jsonObject.add("message", context.serialize(src.getContents()));
    }
    return jsonObject;
  }
}
