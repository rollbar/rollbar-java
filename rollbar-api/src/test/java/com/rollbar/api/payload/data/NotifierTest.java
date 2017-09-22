package com.rollbar.api.payload.data;

import static com.rollbar.test.Factory.notifier;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class NotifierTest {

  @Test
  public void shouldBeEqual() {
    Notifier notifier1 = notifier();
    Notifier notifier2 = notifier();

    assertThat(notifier2, is(notifier1));
  }

  @Test
  public void shouldReturnAsJson() {
    Notifier notifier = notifier();

    Map<String, Object> expected = new HashMap<>();

    if(notifier.getName() != null) expected.put("name", notifier.getName());
    if(notifier.getVersion() != null) expected.put("version", notifier.getVersion());

    assertThat(notifier.asJson(), is(expected));
  }
}