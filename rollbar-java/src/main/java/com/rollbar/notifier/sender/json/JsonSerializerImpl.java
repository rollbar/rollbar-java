package com.rollbar.notifier.sender.json;

import static java.util.regex.Pattern.compile;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Result;
import com.rollbar.notifier.sender.result.ResultCode;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link JsonSerializer json serializer}.
 */
public class JsonSerializerImpl implements JsonSerializer {

  private static final Pattern MESSAGE_PATTERN = compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

  private static final Pattern UUID_PATTERN = compile("\"uuid\"\\s*:\\s*\"([^\"]*)\"");

  private final boolean prettyPrint;

  /**
   * Constructor.
   */
  public JsonSerializerImpl() {
    this(false);
  }

  /**
   * Constructor.
   *
   * @param prettyPrint flag to pretty print the json.
   */
  public JsonSerializerImpl(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  private static void serializeNumber(StringBuilder builder, Number value) {
    builder.append(value);
  }

  private static void serializeBoolean(StringBuilder builder, Boolean value) {
    builder.append(value ? "true" : "false");
  }

  private static void serializeNull(StringBuilder builder) {
    builder.append("null");
  }

  private static void serializeThrowable(StringBuilder builder, Throwable value) {
    final StringWriter writer = new StringWriter();
    value.printStackTrace(new PrintWriter(writer));
    builder.append(String.format("\"%s\"", writer.toString()));
  }

  private static void serializeDefault(StringBuilder builder, Object value) {
    builder.append(String.format("\"%s\"", value));
  }

  private static Map<String, Object> asMap(Map value) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> obj = (Map<String, Object>) value;
      return obj;
    } catch (ClassCastException e) {
      Map<String, Object> obj = new LinkedHashMap<String, Object>();
      for (Object o : value.entrySet()) {
        Map.Entry thisOne = (Map.Entry) o;
        Object key = thisOne.getKey();
        Object val = thisOne.getValue();
        obj.put(key.toString(), val);
      }
      return obj;
    }
  }

  private static void serializeString(StringBuilder builder, String str) {
    builder.append('"');
    builder.append(str.replace("\"", "\\\""));
    builder.append('"');
  }

  private static void indent(StringBuilder builder, int i) {
    for (int x = 0; x <= i; x++) {
      builder.append("  ");
    }
  }

  @Override
  public String toJson(Payload payload) {
    return serialize(payload);
  }

  @Override
  public Result resultFrom(int code, String response) {
    boolean err = code >= 400;
    Pattern p = err ? MESSAGE_PATTERN : UUID_PATTERN;
    Matcher m = p.matcher(response);
    m.find();

    String body = m.group(1);

    return new Result.Builder()
        .code(ResultCode.fromInt(code))
        .body(body)
        .build();
  }

  private String serialize(JsonSerializable object) {
    StringBuilder builder = new StringBuilder();
    serializeValue(builder, object, 0);
    return builder.toString();
  }

  private void serializeObject(Map<String, Object> content, StringBuilder builder, int level) {
    builder.append('{');

    String comma = "";
    for (Map.Entry<String, Object> entry : content.entrySet()) {
      builder.append(comma);
      comma = ",";

      if (prettyPrint) {
        builder.append("\n");
        indent(builder, level);
      }
      serializeString(builder, entry.getKey());

      builder.append(':');
      if (prettyPrint) {
        builder.append(" ");
      }

      serializeValue(builder, entry.getValue(), level + 1);
    }
    if (prettyPrint) {
      builder.append("\n");
    }

    builder.append('}');
  }

  private void serializeValue(StringBuilder builder, Object value, int level) {
    if (value == null) {
      serializeNull(builder);
    } else if (value instanceof Boolean) {
      serializeBoolean(builder, (Boolean) value);
    } else if (value instanceof Number) {
      serializeNumber(builder, (Number) value);
    } else if (value instanceof String) {
      serializeString(builder, (String) value);
    } else if (value instanceof JsonSerializable) {
      serializeValue(builder, ((JsonSerializable) value).asJson(), level);
    } else if (value instanceof Map) {
      Map<String, Object> obj = asMap((Map) value);
      serializeObject(obj, builder, level);
    } else if (value instanceof Collection) {
      serializeArray(builder, ((Collection) value).toArray(), level);
    } else if (value instanceof Object[]) {
      serializeArray(builder, (Object[]) value, level);
    } else if (value instanceof Throwable) {
      serializeThrowable(builder, (Throwable) value);
    } else {
      serializeDefault(builder, value);
    }
  }

  private void serializeArray(StringBuilder builder, Object[] array, int level) {
    builder.append('[');
    String comma = "";
    for (Object obj : array) {
      builder.append(comma);
      comma = ",";

      if (prettyPrint) {
        builder.append("\n");
        indent(builder, level);
      }
      serializeValue(builder, obj, level + 1);
    }
    builder.append(']');
  }


}
