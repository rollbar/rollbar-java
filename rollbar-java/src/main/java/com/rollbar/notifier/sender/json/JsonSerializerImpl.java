package com.rollbar.notifier.sender.json;

import static java.util.regex.Pattern.compile;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Result;
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

  private static final Pattern CODE_PATTERN = compile("\"err\"\\s*:\\s*(\\d)");

  private static final Pattern MESSAGE_PATTERN = compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

  private static final Pattern UUID_PATTERN = compile("\"uuid\"\\s*:\\s*\"([^\"]*)\"");

  private static final String[] REPLACEMENT_CHARS;

  static {
    REPLACEMENT_CHARS = new String[128];
    for (int i = 0; i <= 0x1f; i++) {
      REPLACEMENT_CHARS[i] = String.format("\\u%04x", (int) i);
    }
    REPLACEMENT_CHARS['"'] = "\\\"";
    REPLACEMENT_CHARS['\\'] = "\\\\";
    REPLACEMENT_CHARS['\t'] = "\\t";
    REPLACEMENT_CHARS['\b'] = "\\b";
    REPLACEMENT_CHARS['\n'] = "\\n";
    REPLACEMENT_CHARS['\r'] = "\\r";
    REPLACEMENT_CHARS['\f'] = "\\f";
  }

  private final boolean prettyPrint;

  /**
   * Construct a JsonSerializerImpl that does <b>not</b> pretty print the Payload.
   */
  public JsonSerializerImpl() {
    this(false);
  }

  /**
   * Construct a JsonSerializerImpl.
   * @param prettyPrint whether or not to pretty print the payload.
   */
  public JsonSerializerImpl(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  @Override
  public String toJson(Payload payload) {
    if (payload.json != null) {
      return payload.json;
    }

    StringBuilder builder = new StringBuilder();
    serializeValue(builder, payload, 0);
    return builder.toString();
  }

  @Override
  public Result resultFrom(String response) {
    Matcher codeMatcher = CODE_PATTERN.matcher(response);
    if (!codeMatcher.find()) {
      return new Result.Builder()
          .code(1)
          .body(response)
          .build();
    }
    String codeStr = codeMatcher.group(1);
    int code;
    try {
      code = Integer.parseInt(codeStr);
    } catch (Exception e) {
      code = -1;
    }
    Pattern p;
    if (code == 0) {
      p = UUID_PATTERN;
    } else {
      p = MESSAGE_PATTERN;
    }
    Matcher m = p.matcher(response);
    m.find();

    String body = m.group(1);

    return new Result.Builder()
        .code(code)
        .body(body)
        .build();
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

  private static void serializeThrowable(StringBuilder builder, Throwable value) {
    final StringWriter writer = new StringWriter();
    value.printStackTrace(new PrintWriter(writer));
    serializeString(builder, value.toString());
  }

  private static void serializeDefault(StringBuilder builder, Object value) {
    serializeString(builder, value == null ? "" : value.toString());
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

  private static Map<String, Object> asMap(Map value) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> obj = (Map<String, Object>) value;
      return obj;
    } catch (ClassCastException e) {
      Map<String, Object> obj = new LinkedHashMap<>();
      for (Object o : value.entrySet()) {
        Map.Entry thisOne = (Map.Entry) o;
        Object key = thisOne.getKey();
        Object val = thisOne.getValue();
        obj.put(key.toString(), val);
      }
      return obj;
    }
  }

  // Borrowed from
  // https://github.com/google/gson/blob/59edfc1caf2bb30e30f523f8502f23e8f8edc38e/gson/src/main/java/com/google/gson/stream/JsonWriter.java
  private static void serializeString(StringBuilder builder, String str) {
    builder.append('"');
    int last = 0;
    int length = str.length();
    for (int i = 0; i < length; i++) {
      char c = str.charAt(i);
      String replacement;
      if (c < 128) {
        replacement = REPLACEMENT_CHARS[c];
        if (replacement == null) {
          continue;
        }
      } else if (c == '\u2028') {
        replacement = "\\u2028";
      } else if (c == '\u2029') {
        replacement = "\\u2029";
      } else {
        continue;
      }
      if (last < i) {
        builder.append(str, last, i);
      }
      builder.append(replacement);
      last = i + 1;
    }
    if (last < length) {
      builder.append(str, last, length);
    }
    builder.append('"');
  }

  private static void indent(StringBuilder builder, int i) {
    for (int x = 0; x <= i; x++) {
      builder.append("  ");
    }
  }

}
