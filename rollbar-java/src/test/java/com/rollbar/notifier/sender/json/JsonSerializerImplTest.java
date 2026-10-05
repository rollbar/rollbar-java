package com.rollbar.notifier.sender.json;

import static com.rollbar.notifier.sender.json.JsonTestHelper.assertValidJson;
import static com.rollbar.notifier.sender.json.JsonTestHelper.fromString;
import static com.rollbar.notifier.sender.json.JsonTestHelper.getValue;
import static com.rollbar.notifier.sender.json.JsonSerializerImpl.CIRCULAR_REFERENCE_PLACEHOLDER;
import static com.rollbar.notifier.sender.json.JsonSerializerImpl.MAX_DEPTH_PLACEHOLDER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.notifier.sender.result.Result;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonSerializerImplTest {

  static final String ERROR_MESSAGE = "This is the error message";

  static final String UNEXPECTED_ERROR_MESSAGE = "Nobody expects the Spanish inquisition";

  static final String UUID = java.util.UUID.randomUUID().toString();

  static final String ERROR_RESPONSE = format("{\"err\": 1, {\"message\": \"%s\"}}",
      ERROR_MESSAGE);

  static final String UNEXPECTED_ERROR_RESPONSE = format("<html><body>%s</body></html>",
      UNEXPECTED_ERROR_MESSAGE);

  static final String SUCCESS_RESPONSE = format("{\"err\": 0, {\"uuid\": \"%s\"}}",
      UUID);

  @Test
  public void shouldDeserializeErrorResponse() {
    Result result = new Result.Builder()
        .code(1)
        .body(ERROR_MESSAGE)
        .build();

    JsonSerializerImpl sut = new JsonSerializerImpl();

    assertThat(sut.resultFrom(ERROR_RESPONSE), is(result));
  }

  @Test
  public void shouldDeserializeUnexpectedErrorResponse() {
    Result result = new Result.Builder()
        .code(1)
        .body(UNEXPECTED_ERROR_RESPONSE)
        .build();

    JsonSerializerImpl sut = new JsonSerializerImpl();

    assertThat(sut.resultFrom(UNEXPECTED_ERROR_RESPONSE), is(result));
  }

  @Test
  public void shouldDeserializeSuccessResponse() {
    Result result = new Result.Builder()
        .code(0)
        .body(UUID)
        .build();

    JsonSerializerImpl sut = new JsonSerializerImpl();

    assertThat(sut.resultFrom(SUCCESS_RESPONSE), is(result));
  }

  @Test
  public void shouldDeserializeCustomFormattedResponse() {
    Result expected = new Result.Builder()
            .code(0)
            .body(UUID)
            .build();

    JsonSerializerImpl sut = new JsonSerializerImpl();

    String customResponseJson = SUCCESS_RESPONSE.replace(":", ":\n")
            .replace("}", "\n}");

    Result actual = sut.resultFrom(customResponseJson);
    assertThat(actual, is(expected));
  }

  @Test
  public void shouldSerializeJsonPayload() {
    String json = "{\"foo\":\"bar\"}";

    Payload payload = new Payload(json);

    JsonSerializerImpl sut = new JsonSerializerImpl();

    assertThat(sut.toJson(payload), is(json));
  }

  @Test
  public void shouldSerializeThrowableInCustom() {
    Throwable t = new Throwable() {
      @Override
      public String toString() {
        return "Throwable(\"quoted\")";
      }
    };

    Payload payload = payloadWithCustom("throwable", t);

    JsonSerializerImpl sut = new JsonSerializerImpl();

    String serialized = sut.toJson(payload);

    Map<String, Object> recovered = fromString(serialized);

    String result = getValue(recovered, "data", "custom", "throwable");
    assertThat(result, equalTo("Throwable(\"quoted\")"));
  }

  @Test
  public void shouldSerializeObjectInCustom() {
    Object obj = new Object() {
      @Override
      public String toString() {
        return "Object(\"quoted\")";
      }
    };

    Payload payload = payloadWithCustom("object", obj);

    JsonSerializerImpl sut = new JsonSerializerImpl();

    String serialized = sut.toJson(payload);

    Map<String, Object> recovered = fromString(serialized);

    String result = getValue(recovered, "data", "custom", "object");
    assertThat(result, equalTo("Object(\"quoted\")"));
  }

  @Test
  public void shouldSerializeObjectWhoseToStringReturnsNullAsJsonNull() {
    // The JVMTI agent puts captured local variables into the payload, so toString()
    // here is arbitrary application code. Returning null is a common accident.
    Object obj = new Object() {
      @Override
      public String toString() {
        return null;
      }
    };

    Payload payload = payloadWithCustom("object", obj);

    String serialized = new JsonSerializerImpl().toJson(payload);

    assertValidJson(serialized);
    Map<String, Object> custom = getValue(fromString(serialized), "data", "custom");
    assertThat(custom.containsKey("object"), is(true));
    assertThat(custom.get("object"), is(nullValue()));
  }

  @Test
  public void shouldSerializeObjectWhoseToStringThrows() {
    Object obj = new Object() {
      @Override
      public String toString() {
        throw new IllegalStateException("boom");
      }
    };

    Payload payload = payloadWithCustom("object", obj);

    String serialized = new JsonSerializerImpl().toJson(payload);

    assertValidJson(serialized);
    String result = getValue(fromString(serialized), "data", "custom", "object");
    assertThat(result, equalTo("<toString() threw java.lang.IllegalStateException>"));
  }

  @Test
  public void shouldSerializeThrowableWhoseToStringReturnsNullAsJsonNull() {
    Throwable t = new Throwable() {
      @Override
      public String toString() {
        return null;
      }
    };

    Payload payload = payloadWithCustom("throwable", t);

    String serialized = new JsonSerializerImpl().toJson(payload);

    assertValidJson(serialized);
    Map<String, Object> custom = getValue(fromString(serialized), "data", "custom");
    assertThat(custom.containsKey("throwable"), is(true));
    assertThat(custom.get("throwable"), is(nullValue()));
  }

  @Test
  public void shouldSerializeNullMapValueAsJsonNull() {
    Payload payload = payloadWithCustom("nothing", null);

    String serialized = new JsonSerializerImpl().toJson(payload);

    assertValidJson(serialized);
    Map<String, Object> custom = getValue(fromString(serialized), "data", "custom");
    assertThat(custom.containsKey("nothing"), is(true));
    assertThat(custom.get("nothing"), is(nullValue()));
  }

  @Test
  public void shouldSerializeNullMapKeyWithoutProducingInvalidJson() {
    // HashMap permits a null key, and nothing upstream filters it out.
    Payload payload = payloadWithCustom(null, "a value");

    String serialized = new JsonSerializerImpl().toJson(payload);

    assertValidJson(serialized);
    Map<String, Object> custom = getValue(fromString(serialized), "data", "custom");
    assertThat(custom.get(""), equalTo("a value"));
  }

  @Test
  public void shouldReplaceMapThatContainsItself() {
    Map<String, Object> map = new HashMap<>();
    map.put("self", map);
    map.put("name", "value");

    String serialized = new JsonSerializerImpl().toJson(map);

    assertValidJson(serialized);
    Map<String, Object> result = fromString(serialized);
    assertThat(result.get("self"), equalTo(CIRCULAR_REFERENCE_PLACEHOLDER));
    assertThat(result.get("name"), equalTo("value"));
  }

  @Test
  public void shouldReplaceCycleThroughNestedCollections() {
    Map<String, Object> parent = new LinkedHashMap<>();
    List<Object> children = new ArrayList<>();
    Map<String, Object> child = new LinkedHashMap<>();
    child.put("parent", parent);
    children.add(child);
    parent.put("children", children);

    String serialized = new JsonSerializerImpl().toJson(payloadWithCustom("parent", parent));

    assertValidJson(serialized);
    List<Object> serializedChildren = getValue(fromString(serialized),
        "data", "custom", "parent", "children");
    Map<?, ?> serializedChild = (Map<?, ?>) serializedChildren.get(0);
    assertThat(serializedChild.get("parent"), equalTo(CIRCULAR_REFERENCE_PLACEHOLDER));
  }

  @Test
  public void shouldReplaceCollectionThatContainsItself() {
    List<Object> list = new ArrayList<>();
    list.add("first");
    list.add(list);

    String serialized = new JsonSerializerImpl().toJson(payloadWithCustom("list", list));

    assertValidJson(serialized);
    List<Object> result = getValue(fromString(serialized), "data", "custom", "list");
    assertThat(result, equalTo(asList((Object) "first", CIRCULAR_REFERENCE_PLACEHOLDER)));
  }

  @Test
  public void shouldReplaceArrayThatContainsItself() {
    Object[] array = new Object[2];
    array[0] = "first";
    array[1] = array;

    String serialized = new JsonSerializerImpl().toJson(payloadWithCustom("array", array));

    assertValidJson(serialized);
    List<Object> result = getValue(fromString(serialized), "data", "custom", "array");
    assertThat(result, equalTo(asList((Object) "first", CIRCULAR_REFERENCE_PLACEHOLDER)));
  }

  @Test
  public void shouldReplaceJsonSerializableThatContainsItself() {
    SelfReferencing value = new SelfReferencing();

    String serialized = new JsonSerializerImpl().toJson(payloadWithCustom("value", value));

    assertValidJson(serialized);
    Map<String, Object> result = getValue(fromString(serialized), "data", "custom", "value");
    assertThat(result.get("self"), equalTo(CIRCULAR_REFERENCE_PLACEHOLDER));
  }

  @Test
  public void shouldSerializeSharedValueEveryTimeItIsReached() {
    Map<String, Object> shared = new HashMap<>();
    shared.put("key", "value");
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("first", shared);
    map.put("second", shared);

    String serialized = new JsonSerializerImpl().toJson(map);

    assertValidJson(serialized);
    Map<String, Object> result = fromString(serialized);
    assertThat(result.get("first"), equalTo(shared));
    assertThat(result.get("second"), equalTo(shared));
  }

  @Test
  public void shouldReplaceValuesNestedDeeperThanMaxDepth() {
    Map<String, Object> innermost = new HashMap<>();
    innermost.put("key", "value");
    Map<String, Object> middle = new HashMap<>();
    middle.put("innermost", innermost);
    Map<String, Object> outer = new HashMap<>();
    outer.put("middle", middle);
    outer.put("list", asList("a", "b"));

    String serialized = new JsonSerializerImpl(false, 2).toJson(outer);

    assertValidJson(serialized);
    Map<String, Object> result = fromString(serialized);
    assertThat(result.get("list"), equalTo(asList("a", "b")));
    Map<?, ?> serializedMiddle = (Map<?, ?>) result.get("middle");
    assertThat(serializedMiddle.get("innermost"), equalTo(MAX_DEPTH_PLACEHOLDER));
  }

  @Test
  public void shouldLimitDeeplyNestedValuesToDefaultMaxDepth() {
    Map<String, Object> root = new HashMap<>();
    Map<String, Object> current = root;
    for (int i = 0; i < 100_000; i++) {
      Map<String, Object> next = new HashMap<>();
      current.put("next", next);
      current = next;
    }

    String serialized = new JsonSerializerImpl().toJson(root);

    assertValidJson(serialized);
    Object value = fromString(serialized);
    int depth = 0;
    while (value instanceof Map) {
      value = ((Map<?, ?>) value).get("next");
      depth++;
    }
    assertThat(depth, is(JsonSerializerImpl.DEFAULT_MAX_DEPTH));
    assertThat(value, equalTo(MAX_DEPTH_PLACEHOLDER));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectMaxDepthBelowOne() {
    new JsonSerializerImpl(false, 0);
  }

  private static class SelfReferencing implements JsonSerializable {
    @Override
    public Object asJson() {
      Map<String, Object> json = new HashMap<>();
      json.put("self", this);
      return json;
    }
  }

  private Payload payloadWithCustom(String key, Object value) {
    Map<String, Object> custom = new HashMap<>();
    custom.put(key, value);

    return new Payload.Builder()
            .data(new Data.Builder()
                    .custom(custom).build()).build();
  }
}
