package com.rollbar.notifier.util.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.rollbar.api.payload.data.Level;
import java.lang.reflect.Type;

public class LevelSerializer implements JsonSerializer<Level> {

  @Override
  public JsonElement serialize(Level src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.name().toLowerCase());
  }
}
