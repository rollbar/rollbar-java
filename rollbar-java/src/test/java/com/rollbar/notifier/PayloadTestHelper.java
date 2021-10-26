package com.rollbar.notifier;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.body.BodyContent;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class PayloadTestHelper {
  @SuppressWarnings("unchecked")
  public static <T extends BodyContent> T getBodyContentAs(Class<T> clazz, Payload payload) {
    assertThat("Payload should not be null", payload, not(nullValue()));
    assertThat("Data should not be null", payload.getData(), not(nullValue()));
    assertThat("Body should not be null", payload.getData().getBody(), not(nullValue()));

    BodyContent content = payload.getData().getBody().getContents();
    assertThat(content, instanceOf(clazz));
    return (T)content;
  }
}
