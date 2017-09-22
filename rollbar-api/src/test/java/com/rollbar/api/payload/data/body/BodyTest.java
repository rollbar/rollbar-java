package com.rollbar.api.payload.data.body;

import static com.rollbar.test.Factory.body;
import static com.rollbar.test.Factory.crashReport;
import static com.rollbar.test.Factory.message;
import static com.rollbar.test.Factory.trace;
import static com.rollbar.test.Factory.traceChain;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BodyTest {

  @Test
  public void shouldBeEqual() {
    Body messageBody1 = body(message());
    Body messageBody2 = body(message());
    assertThat(messageBody2, is(messageBody1));

    Body crashBody1 = body(crashReport());
    Body crashBody2 = body(crashReport());
    assertThat(crashBody2, is(crashBody1));

    Body traceBody1 = body(trace());
    Body traceBody2 = body(trace());
    assertThat(traceBody2, is(traceBody1));

    Body traceChainBody1 = body(traceChain());
    Body traceChainBody2 = body(traceChain());
    assertThat(traceChainBody2, is(traceChainBody1));
  }

  @Test
  public void shouldReturnAsJsonMessage() {
    Message message = message();
    Body messageBody = body(message);

    Map<String, Object> expected = new HashMap<>();
    expected.put(message.getKeyName(), message);

    assertThat(messageBody.asJson(), is(expected));
  }


  @Test
  public void shouldReturnAsJsonCrashReport() {
    CrashReport crashReport = crashReport();
    Body crashReportBody = body(crashReport);

    Map<String, Object> expected = new HashMap<>();
    expected.put(crashReport.getKeyName(), crashReport);

    assertThat(crashReportBody.asJson(), is(expected));
  }

  @Test
  public void shouldReturnAsJsonTrace() {
    Trace trace = trace();
    Body traceBody = body(trace);

    Map<String, Object> expected = new HashMap<>();
    expected.put(trace.getKeyName(), trace);

    assertThat(traceBody.asJson(), is(expected));
  }

  @Test
  public void shouldReturnAsJsonTraceChain() {
    TraceChain traceChain = traceChain();
    Body traceChainbody = body(traceChain);

    Map<String, Object> expected = new HashMap<>();
    expected.put(traceChain.getKeyName(), traceChain);

    assertThat(traceChainbody.asJson(), is(expected));
  }
}