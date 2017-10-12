package com.rollbar.api.payload.data.body;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.rollbar.test.Factory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class FrameTest {

  @Test
  public void shouldBeEqual() {
    Frame frame1 = Factory.frame();
    Frame frame2 = Factory.frame();

    assertEquals(frame1, frame2);
  }

  @Test
  public void shouldReturnAsJson() {
    Frame frame = Factory.frame();

    Map<String, Object> expected = new HashMap<>();
    if (frame.getFilename() != null) {
      expected.put("filename", frame.getFilename());
    }
    if (frame.getLineNumber() != null) {
      expected.put("lineno", frame.getLineNumber());
    }
    if (frame.getColumnNumber() != null) {
      expected.put("colno", frame.getColumnNumber());
    }
    if (frame.getMethod() != null) {
      expected.put("method", frame.getMethod());
    }
    if (frame.getCode() != null) {
      expected.put("code", frame.getCode());
    }
    if (frame.getClassName() != null) {
      expected.put("class_name", frame.getClassName());
    }
    if (frame.getContext() != null) {
      expected.put("context", frame.getContext());
    }
    if (frame.getArgs() != null) {
      expected.put("args", frame.getArgs());
    }
    if (frame.getKeywordArgs() != null) {
      expected.put("kwargs", frame.getKeywordArgs());
    }

    assertThat(frame.asJson(), is(expected));
  }
}