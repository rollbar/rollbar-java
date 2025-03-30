package com.rollbar.notifier.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.TelemetryType;
import com.rollbar.api.payload.data.body.*;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;

import java.util.*;

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
    assertNull(map.get("group"));
  }

  @Test
  public void shouldTruncateTelemetryEvents() {
    Map<String, String> telemetryBody = new HashMap<>();
    telemetryBody.put("message", "1234567890");
    TelemetryEvent telemetryEvent = makeTelemetryEvent(telemetryBody);
    ArrayList<TelemetryEvent> telemetryEvents = new ArrayList<>();
    telemetryEvents.add(telemetryEvent);

    Body body = sut.from(null, DESCRIPTION, telemetryEvents).truncateStrings(5);

    telemetryBody.put("message", "12345");
    TelemetryEvent expected = makeTelemetryEvent(telemetryBody);
    assertEquals(expected, getFirstTelemetryEvent(body));
  }

  @Test
  public void shouldTruncateGroups() {
    int expectedLength = 5;
    Throwable throwable = buildSimpleThrowable();
    ThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(throwable, Thread.currentThread());

    Body truncatedBody = sut.from(throwableWrapper, DESCRIPTION).truncateStrings(expectedLength);

    Trace trace = getFirstTraceFromGroup(truncatedBody);
    assertEquals(expectedLength, trace.getException().getMessage().length());
    assertEquals(expectedLength, trace.getException().getClassName().length());
    assertEquals(expectedLength, trace.getException().getDescription().length());
    assertEquals(expectedLength, trace.getFrames().get(0).getClassName().length());
  }

  @Test
  public void shouldBuildBodyWithThreads() {
    Throwable throwable = buildSimpleThrowable();
    ThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(throwable, Thread.currentThread());
    Body body = sut.from(throwableWrapper, DESCRIPTION);

    assertThat(body.getContents(), is(instanceOf(Trace.class)));
    HashMap<String, Object> map = (HashMap<String, Object>) body.asJson();
    assertNull(map.get("telemetry"));
    assertNotNull(map.get("group"));
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

  private TelemetryEvent makeTelemetryEvent( Map<String, String> body) {
    return new TelemetryEvent(TelemetryType.LOG, Level.DEBUG, 12L, Source.CLIENT, body);
  }

  private TelemetryEvent getFirstTelemetryEvent(Body body) {
    HashMap<String, Object> map = (HashMap<String, Object>) body.asJson();
    List<TelemetryEvent> telemetryEvents = (List<TelemetryEvent>) map.get("telemetry");
    return telemetryEvents.get(0);
  }

  private Trace getFirstTraceFromGroup(Body body) {
    HashMap<String, Object> bodyJson = (HashMap<String, Object>) body.asJson();
    List<Group> groups = (List<Group>) bodyJson.get("group");

    HashMap<String, Object> groupJson = (HashMap<String, Object>) groups.get(0).asJson();
    List<RollbarThread> rollbarThreads = (List<RollbarThread>) groupJson.get("threads");

    HashMap<String, Object> rollbarThreadJson = (HashMap<String, Object>) rollbarThreads.get(0).asJson();
    TraceChain traceChain = (TraceChain) rollbarThreadJson.get("trace_chain");
    return traceChain.getTraces().get(0);
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
