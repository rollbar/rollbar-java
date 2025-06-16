package com.rollbar.android.anr.historical.stacktrace;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;

public class StackFrameTest {
  private static final String MODULE = "any module";
  private static final String PACKAGE = "any package";
  private static final String FILENAME = "any filename";
  private static final String FUNCTION = "any function";
  private static final int LINE_NO = 20;

  @Test
  public void jsonRepresentationShouldBeTheExpected() {
    StackFrame stackFrame = givenAStackFrame();

    HashMap<String, Object> json = getJson(stackFrame);

    assertEquals(expectedJson(), json);
  }

  @Test
  public void jsonRepresentationShouldBeAnEmptyMapIfAllPropertiesAreNull() {
    StackFrame stackFrame = givenAStackFrameWithNullProperties();

    HashMap<String, Object> json = getJson(stackFrame);

    assertEquals(new HashMap<>(), json);
  }

  @SuppressWarnings("unchecked")
  private HashMap<String, Object> getJson(StackFrame stackFrame) {
    return (HashMap<String, Object>) stackFrame.asJson();
  }

  private StackFrame givenAStackFrame() {
    StackFrame stackFrame = new StackFrame();
    stackFrame.setModule(MODULE);
    stackFrame.setPackage(PACKAGE);
    stackFrame.setFilename(FILENAME);
    stackFrame.setFunction(FUNCTION);
    stackFrame.setLineno(LINE_NO);
    return stackFrame;
  }

  private HashMap<String, Object> expectedJson() {
    HashMap<String, Object> json = new HashMap<>();

    json.put("module", MODULE);
    json.put("package", PACKAGE);
    json.put("filename", FILENAME);
    json.put("function", FUNCTION);
    json.put("lineno", LINE_NO);

    return json;
  }

  private StackFrame givenAStackFrameWithNullProperties() {
    StackFrame stackFrame = new StackFrame();
    stackFrame.setModule(null);
    stackFrame.setPackage(null);
    stackFrame.setFilename(null);
    stackFrame.setFunction(null);
    stackFrame.setLineno(null);
    return stackFrame;
  }

}
