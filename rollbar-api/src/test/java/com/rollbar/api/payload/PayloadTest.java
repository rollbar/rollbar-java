package com.rollbar.api.payload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.rollbar.api.payload.data.Data;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class PayloadTest {

  static final String ACCESS_TOKEN = "asdf33324fddsf";

  static final Data DATA = new Data.Builder().build();

  @Test
  public void shouldBuild() {
    Payload payload = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(DATA)
        .build();

    assertEquals(ACCESS_TOKEN, payload.getAccessToken());
    assertEquals(DATA, payload.getData());
  }

  @Test
  public void shouldBeEqual() {
    Payload payload1 = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(DATA)
        .build();

    Payload payload2 = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(DATA)
        .build();

    assertEquals(payload1, payload2);
  }

  @Test
  public void shouldReturnAsJson() {
    Payload payload = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(DATA)
        .build();

    Map<String, Object> expected = new HashMap<>();
    expected.put("access_token", ACCESS_TOKEN);
    expected.put("data", DATA);

    assertEquals(expected, payload.asJson());
  }

  @Test
  public void toStringShouldNotContainAccessToken() {
    Payload payload = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(DATA)
        .build();

    String result = payload.toString();

    assertFalse(result.contains(ACCESS_TOKEN));
    assertTrue(result.contains("accessToken='****ddsf'"));
  }

  @Test
  public void toStringShouldFullyMaskShortAccessToken() {
    Payload payload = new Payload.Builder()
        .accessToken("abcd1234")
        .build();

    String result = payload.toString();

    assertFalse(result.contains("1234"));
    assertTrue(result.contains("accessToken='****'"));
  }

  @Test
  public void toStringShouldHandleNullAccessToken() {
    Payload payload = new Payload.Builder()
        .data(DATA)
        .build();

    assertTrue(payload.toString().contains("accessToken=null"));
  }

  @Test
  public void shouldReturnJsonAsJson() {
    String json = "{\"foo\":\"bar\"}";

    Payload payload = new Payload(json);

    assertEquals(json, payload.json);
  }
}
