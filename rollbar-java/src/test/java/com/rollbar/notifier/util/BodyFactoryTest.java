package com.rollbar.notifier.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.ExceptionInfo;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Message;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class BodyFactoryTest {

  static final String MESSAGE_ERROR = "Something went wrong.";

  static final String NESTED_MESSAGE_ERROR = "Nested message error";

  static final String DESCRIPTION = "This is the error description";

  BodyFactory sut;

  @Before
  public void setUp() {
    sut = new BodyFactory();
  }

  @Test
  public void shouldBuildBodyWithDescription() {
    Body body1 = sut.from((Throwable) null, DESCRIPTION);

    assertThat(body1.getContents(), is(instanceOf(Message.class)));
    assertThat(((Message) body1.getContents()).getBody(), is(DESCRIPTION));

    Body body2 = sut.from((ThrowableWrapper) null, DESCRIPTION);

    assertThat(body2.getContents(), is(instanceOf(Message.class)));
    assertThat(((Message) body2.getContents()).getBody(), is(DESCRIPTION));
  }

  @Test
  public void shouldBuildBodyWithDescriptionAndTelemetryEvents() {
    Body body = sut.from(null, DESCRIPTION, new ArrayList<>());

    assertThat(body.getContents(), is(instanceOf(Message.class)));
    assertThat(((Message) body.getContents()).getBody(), is(DESCRIPTION));
    HashMap<String, Object> map = (HashMap<String, Object>) body.asJson();
    assertNotNull(map.get("telemetry"));
    assertNull(map.get("threads"));
  }

  @Test
  public void shouldBuildBodyWithThreads() {
    Throwable throwable = buildSimpleThrowable();
    ThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(throwable, Thread.currentThread());
    Body body = sut.from(throwableWrapper, DESCRIPTION);

    assertThat(body.getContents(), is(instanceOf(Trace.class)));
    HashMap<String, Object> map = (HashMap<String, Object>) body.asJson();
    assertNull(map.get("telemetry"));
    assertNotNull(map.get("threads"));
  }

  @Test
  public void shouldBuildBodyWithTraceAsContent() {
    Throwable throwable = buildSimpleThrowable();
    Body body = sut.from(throwable, DESCRIPTION);

    assertThat(body.getContents(), is(instanceOf(Trace.class)));
    verifyTrace((Trace) body.getContents(), throwable, DESCRIPTION);
  }

  @Test
  public void shouldBuildBodyWithTraceChainAsContent() {
    Throwable throwable = buildNestedThrowable();

    Body body = sut.from(throwable, DESCRIPTION);

    assertThat(body.getContents(), is(instanceOf(TraceChain.class)));
    verifyTraceChain((TraceChain) body.getContents(), throwable);
  }

  @Test
  public void shouldBuildBodyWithTraceAsContentFromThrowableWrapper() {
    Throwable throwable = buildSimpleThrowable();
    ThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(throwable);

    Body body = sut.from(throwableWrapper, DESCRIPTION);

    assertThat(body.getContents(), is(instanceOf(Trace.class)));
    verifyTrace((Trace) body.getContents(), throwable, DESCRIPTION);
  }

  @Test
  public void shouldBuildBodyWithTraceChainAsContentFromThrowableWrapper() {
    Throwable throwable = buildNestedThrowable();
    ThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(throwable);

    Body body = sut.from(throwableWrapper, DESCRIPTION);

    assertThat(body.getContents(), is(instanceOf(TraceChain.class)));
    verifyTraceChain((TraceChain) body.getContents(), throwable);
  }

  private void verifyTrace(Trace trace, Throwable throwable, String description) {
    verifyFrames(trace.getFrames(), throwable);
    verifyExceptionInfo(trace.getException(), throwable, description);
  }

  private void verifyTraceChain(TraceChain traceChain, Throwable throwable) {
    List<Trace> traces = traceChain.getTraces();
    String description = DESCRIPTION;

    for(Trace trace : traces) {
      verifyTrace(trace, throwable, description);
      throwable = throwable.getCause();
      description = null;
    }
  }

  private void verifyFrames(List<Frame> frames, Throwable throwable) {
    List<Frame> expected = getFrames(throwable);

    assertThat(frames, is(expected));
  }

  private void verifyExceptionInfo(ExceptionInfo exceptionInfo, Throwable throwable,
      String description) {
    ExceptionInfo expected = new ExceptionInfo.Builder()
        .className(RuntimeException.class.getName())
        .message(throwable.getMessage())
        .description(description)
        .build();

    assertThat(exceptionInfo, is(expected));
  }

  private Throwable buildSimpleThrowable() {
    return new RuntimeException(MESSAGE_ERROR);
  }

  private Throwable buildNestedThrowable() {
    Throwable nestedError = new RuntimeException(NESTED_MESSAGE_ERROR);

    return new RuntimeException(MESSAGE_ERROR, nestedError);
  }

  private static List<Frame> getFrames(Throwable throwable) {
    StackTraceElement[] elements = throwable.getStackTrace();
    ArrayList<Frame> result = new ArrayList<Frame>();
    for (StackTraceElement element : elements) {
      result.add(getFrame(element));
    }
    Collections.reverse(result);
    return result;
  }

  private static Frame getFrame(StackTraceElement element) {
    return new Frame.Builder()
        .filename(element.getFileName())
        .lineNumber(element.getLineNumber())
        .method(element.getMethodName())
        .className(element.getClassName())
        .build();
  }
}
