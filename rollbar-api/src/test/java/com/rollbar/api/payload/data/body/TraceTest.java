package com.rollbar.api.payload.data.body;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.rollbar.test.Factory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TraceTest {

  @Test
  public void shouldBeEqual() {
    Trace trace1 = Factory.trace();
    Trace trace2 = Factory.trace();

    assertThat(trace2, is(trace1));
  }

  @Test
  public void shouldReturnAsJson() {
    Trace trace = Factory.trace();

    Map<String, Object> expected = new HashMap<>();
    expected.put("frames", trace.getFrames());
    expected.put("exception", trace.getException());

    assertThat(trace.asJson(), is(expected));
  }
}