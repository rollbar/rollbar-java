package com.rollbar.api.payload.data.body;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.rollbar.test.Factory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ExceptionInfoTest {

  @Test
  public void shouldBeEqual() {
    ExceptionInfo exceptionInfo1 = Factory.exceptionInfo();
    ExceptionInfo exceptionInfo2 = Factory.exceptionInfo();

    assertThat(exceptionInfo2, is(exceptionInfo1));
  }

  @Test
  public void shouldReturnAsJson() {
    ExceptionInfo exceptionInfo = Factory.exceptionInfo();

    Map<String, Object> expected = new HashMap<>();
    expected.put("class", exceptionInfo.getClassName());
    expected.put("message", exceptionInfo.getMessage());
    expected.put("description", exceptionInfo.getDescription());

    assertThat(exceptionInfo.asJson(), is(expected));
  }
}