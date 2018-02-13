package com.rollbar.notifier.util.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.api.payload.data.Data;

import java.lang.reflect.Type;
import java.util.Map;

public class DataSerializer implements JsonSerializer<Data> {

  @Override
  public JsonElement serialize(Data src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    for(Map.Entry<String, Object> entry : src.asJson().entrySet()) {
      jsonObject.add(entry.getKey(), context.serialize(entry.getValue()));
    }

    return jsonObject;
  }
}
