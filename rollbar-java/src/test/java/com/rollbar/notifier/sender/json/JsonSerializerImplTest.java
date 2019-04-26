package com.rollbar.notifier.sender.json;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Result;
import org.junit.Test;

public class JsonSerializerImplTest {

  static final String ERROR_MESSAGE = "This is the error message";

  static final String UUID = java.util.UUID.randomUUID().toString();

  static final String ERROR_RESPONSE = format("{\"err\": 1, {\"message\": \"%s\"}}",
      ERROR_MESSAGE);

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
  public void shouldDeserializeSuccessResponse() {
    Result result = new Result.Builder()
        .code(0)
        .body(UUID)
        .build();

    JsonSerializerImpl sut = new JsonSerializerImpl();

    assertThat(sut.resultFrom(SUCCESS_RESPONSE), is(result));
  }

  @Test
  public void shouldSerializeJsonPayload() {
    String json = "{\"foo\":\"bar\"}";

    Payload payload = new Payload(json);

    JsonSerializerImpl sut = new JsonSerializerImpl();

    assertThat(sut.toJson(payload), is(json));
  }
}
