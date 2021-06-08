package com.rollbar.notifier.sender.json;

import static com.rollbar.notifier.sender.json.JsonTestHelper.fromString;
import static com.rollbar.notifier.sender.json.JsonTestHelper.getValue;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.notifier.sender.result.Result;
import org.junit.Test;

import java.util.HashMap;
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

  private Payload payloadWithCustom(String key, Object value) {
    Map<String, Object> custom = new HashMap<>();
    custom.put(key, value);

    return new Payload.Builder()
            .data(new Data.Builder()
                    .custom(custom).build()).build();
  }
}
