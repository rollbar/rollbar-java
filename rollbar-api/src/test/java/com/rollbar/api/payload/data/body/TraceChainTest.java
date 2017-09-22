package com.rollbar.api.payload.data.body;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.rollbar.test.Factory;
import org.junit.Test;

public class TraceChainTest {

  @Test
  public void shouldBeEqual() {
    TraceChain traceChain1 = Factory.traceChain();
    TraceChain traceChain2 = Factory.traceChain();

    assertThat(traceChain2, is(traceChain1));
  }

  @Test
  public void shouldReturnAsJson() {
    TraceChain traceChain = Factory.traceChain();

    assertThat(traceChain.getKeyName(), is("trace_chain"));

    assertThat(traceChain.asJson(), is(traceChain.getTraces()));
  }
}