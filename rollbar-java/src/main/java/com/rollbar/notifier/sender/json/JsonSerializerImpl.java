package com.rollbar.notifier.sender.json;

import static java.util.regex.Pattern.compile;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Result;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link JsonSerializer json serializer}.
 *
 * <p>
 * Payloads carry arbitrary user data, such as custom maps and captured local variables, so the
 * serializer guards against object graphs that would otherwise make it recurse without bound. A
 * map, collection, array or {@link JsonSerializable} that refers back to one of its enclosing
 * values is replaced by {@value #CIRCULAR_REFERENCE_PLACEHOLDER}, and one nested deeper than
 * {@code maxDepth} levels is replaced by {@value #MAX_DEPTH_PLACEHOLDER}.
 * </p>
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

  /**
   * The default maximum nesting depth, see {@link #JsonSerializerImpl(boolean, int)}.
   */
  public static final int DEFAULT_MAX_DEPTH = 100;

  static final String CIRCULAR_REFERENCE_PLACEHOLDER = "<circular reference>";

  static final String MAX_DEPTH_PLACEHOLDER = "<max depth exceeded>";

  private final boolean prettyPrint;

  private final int maxDepth;

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
    this(prettyPrint, DEFAULT_MAX_DEPTH);
  }

  /**
   * Construct a JsonSerializerImpl.
   * @param prettyPrint whether or not to pretty print the payload.
   * @param maxDepth the maximum number of nested maps, collections and arrays to serialize,
   *                 counting the outermost object. Anything nested deeper is replaced by a
   *                 placeholder string. Must be at least 1.
   */
  public JsonSerializerImpl(boolean prettyPrint, int maxDepth) {
    if (maxDepth < 1) {
      throw new IllegalArgumentException("The max depth must be at least 1, got " + maxDepth);
    }
    this.prettyPrint = prettyPrint;
    this.maxDepth = maxDepth;
  }

  @Override
  public String toJson(Payload payload) {
    if (payload.json != null) {
      return payload.json;
    }

    return toJson(payload.asJson());
  }

  /**
   * Converts the map to a JSON string.
   * @param map The map to be converted to a JSON string.
   * @return A JSON string that represents the map.
   */
  public String toJson(Map<String, Object> map) {
    StringBuilder builder = new StringBuilder();
    // Identity based: the values being tracked are the ones that may contain themselves, and the
    // equals() and hashCode() of such a map or collection recurse forever as well.
    Set<Object> visiting = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    serializeValue(builder, map, 0, visiting);
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


  private void serializeObject(Map<String, Object> content, StringBuilder builder, int level,
                               Set<Object> visiting) {
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

      serializeValue(builder, entry.getValue(), level + 1, visiting);
    }
    if (prettyPrint) {
      builder.append("\n");
    }

    builder.append('}');
  }

  private void serializeValue(StringBuilder builder, Object value, int level,
                              Set<Object> visiting) {
    if (value == null) {
      serializeNull(builder);
    } else if (value instanceof Boolean) {
      serializeBoolean(builder, (Boolean) value);
    } else if (value instanceof Number) {
      serializeNumber(builder, (Number) value);
    } else if (value instanceof String) {
      serializeString(builder, (String) value);
    } else if (value instanceof JsonSerializable || value instanceof Map
        || value instanceof Collection || value instanceof Object[]) {
      serializeNested(builder, value, level, visiting);
    } else if (value instanceof Throwable) {
      serializeThrowable(builder, (Throwable) value);
    } else {
      serializeDefault(builder, value);
    }
  }

  /**
   * Serializes a value that can contain other values, unless doing so would recurse without
   * bound. {@code visiting} holds the values enclosing this one, so finding the value there means
   * it contains itself. Values are removed again on the way out, so the same value reached twice
   * through different paths, without being its own ancestor, is serialized both times.
   */
  private void serializeNested(StringBuilder builder, Object value, int level,
                               Set<Object> visiting) {
    if (visiting.contains(value)) {
      serializeString(builder, CIRCULAR_REFERENCE_PLACEHOLDER);
      return;
    }
    if (level >= maxDepth) {
      serializeString(builder, MAX_DEPTH_PLACEHOLDER);
      return;
    }

    visiting.add(value);
    try {
      if (value instanceof JsonSerializable) {
        // Not a level of its own: it stands for the value it converts to.
        serializeValue(builder, ((JsonSerializable) value).asJson(), level, visiting);
      } else if (value instanceof Map) {
        Map<String, Object> obj = asMap((Map) value);
        serializeObject(obj, builder, level, visiting);
      } else if (value instanceof Collection) {
        serializeArray(builder, ((Collection) value).toArray(), level, visiting);
      } else {
        serializeArray(builder, (Object[]) value, level, visiting);
      }
    } finally {
      visiting.remove(value);
    }
  }

  private static void serializeThrowable(StringBuilder builder, Throwable value) {
    serializeToString(builder, value);
  }

  private static void serializeDefault(StringBuilder builder, Object value) {
    serializeToString(builder, value);
  }

  /**
   * Serializes an arbitrary object via toString(). The value is never null here -
   * serializeValue handles that case - but toString() itself is application code and
   * may return null or throw, and neither may be allowed to break the whole payload.
   * This matters because the JVMTI agent puts captured local variables, i.e. arbitrary
   * application objects, into the payload.
   */
  private static void serializeToString(StringBuilder builder, Object value) {
    String str;
    try {
      str = value.toString();
    } catch (Exception e) {
      // Deliberately not catching Error: a StackOverflowError from a recursive
      // toString() should propagate rather than be silently swallowed here.
      serializeString(builder, "<toString() threw " + e.getClass().getName() + ">");
      return;
    }
    serializeNullableString(builder, str);
  }

  /**
   * Serializes a string in value position, emitting the JSON null literal when it is
   * null. Not usable for object keys, which must always be quoted - see
   * {@link #serializeString}.
   *
   * <p>The null check lives here rather than inline in {@link #serializeToString}
   * because {@code Object.toString()} is declared non-null, so static analysis reads a
   * check on its result as redundant. An overriding implementation can still return
   * null at runtime, which is exactly the case being handled.
   */
  private static void serializeNullableString(StringBuilder builder, String str) {
    if (str == null) {
      serializeNull(builder);
      return;
    }
    serializeString(builder, str);
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

  private void serializeArray(StringBuilder builder, Object[] array, int level,
                              Set<Object> visiting) {
    builder.append('[');
    String comma = "";
    for (Object obj : array) {
      builder.append(comma);
      comma = ",";

      if (prettyPrint) {
        builder.append("\n");
        indent(builder, level);
      }
      serializeValue(builder, obj, level + 1, visiting);
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
    // Emit an empty string rather than the bare null literal: this is also used for
    // object keys, where an unquoted null would be invalid JSON. Callers that want a
    // real JSON null for a null value route to serializeNull themselves.
    if (str == null) {
      builder.append("\"\"");
      return;
    }

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
