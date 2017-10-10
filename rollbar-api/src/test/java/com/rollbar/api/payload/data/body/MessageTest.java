package com.rollbar.api.payload.data.body;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.rollbar.test.Factory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MessageTest {

  @Test
  public void shouldBeEqual() {
    Message message1 = Factory.message();
    Message message2 = Factory.message();

    assertThat(message2, is(message1));
  }

  @Test
  public void shouldReturnAsJson() {
    Message message = Factory.message();
    Map<String, String> bodyMap = new HashMap<>();
    bodyMap.put("body", message.getBody());
    Object expected = bodyMap;

    assertThat(message.getKeyName(), is("message"));
    assertThat(message.asJson(), is(expected));
  }
}