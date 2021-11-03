package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.body.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.rollbar.notifier.PayloadTestHelper.getBodyContentAs;
import static com.rollbar.notifier.truncation.TestPayloadBuilder.makeString;
import static com.rollbar.notifier.truncation.TruncationMatchers.differsOnlyBy;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public abstract class MinBodyStrategyTest {
  protected final MinBodyStrategy sut = new MinBodyStrategy();

  public static class GeneralTests extends MinBodyStrategyTest {
    private TestPayloadBuilder payloadBuilder;

    @Before
    public void setUp() {
      payloadBuilder = new TestPayloadBuilder();
    }

    @Test
    public void ifBodyIsNullItShouldNotTruncate() {
      Payload payload = payloadBuilder.createTestPayload((Body) null);

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }

    @Test
    public void ifDataIsNullItShouldNotTruncate() {
      Payload payload = new Payload.Builder(payloadBuilder.createTestPayload())
          .data(null)
          .build();

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }

    @Test
    public void ifBodyContainsNoTraceItShouldNotTruncate() {
      Payload payload = payloadBuilder.createTestPayload(
          new Body.Builder()
              .bodyContent(new Message.Builder().body("A message").build())
              .build()
      );

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }

    @Test
    public void ifExceptionAndTraceAreTooSmallToBeTruncatedItShouldNotTruncate() {
      List<Frame> frames = payloadBuilder.createFrames(2);
      Trace trace = new Trace.Builder()
          .frames(frames)
          .exception(new ExceptionInfo.Builder()
              .message("less than 255 chars")
              .description(null)
              .build())
          .build();

      Payload payload = payloadBuilder.createTestPayloadSingleTrace(trace);

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }

    @Test
    public void ifExceptionAndChainAreTooSmallToBeTruncatedItShouldNotTruncate() {
      Trace trace1 = new Trace.Builder()
          .frames(payloadBuilder.createFrames(2))
          .exception(new ExceptionInfo.Builder()
              .message("fewer than 255 chars")
              .description(null)
              .build())
          .build();

      Trace trace2 = new Trace.Builder()
          .frames(payloadBuilder.createFrames(1))
          .exception(new ExceptionInfo.Builder()
              .message("even fewer")
              .description(null)
              .build())
          .build();

      TraceChain chain = new TraceChain.Builder()
          .traces(Arrays.asList(trace1, trace2))
          .build();

      Payload payload = payloadBuilder.createTestPayload(
          new Body.Builder().bodyContent(chain).build()
      );

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }

    @Test
    public void ifSomeTracesAreTooLargeItShouldTruncate() {
      Trace smallTrace = new Trace.Builder()
          .frames(payloadBuilder.createFrames(1))
          .exception(new ExceptionInfo.Builder()
              .message("fewer than 255 chars")
              .description(null)
              .build())
          .build();

      Trace largeTrace = new Trace.Builder()
          .frames(payloadBuilder.createFrames(30))
          .exception(new ExceptionInfo.Builder()
              .message("still fewer than 255")
              .description(null)
              .build())
          .build();

      TraceChain chain = new TraceChain.Builder()
          .traces(Arrays.asList(smallTrace, largeTrace))
          .build();

      Payload payload = payloadBuilder.createTestPayload(
          new Body.Builder().bodyContent(chain).build()
      );

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(true));

      TraceChain updatedChain = getBodyContentAs(TraceChain.class, result.value);

      assertThat(updatedChain.getTraces(), hasSize(2));
      assertThat(updatedChain.getTraces().get(0).getFrames(), hasSize(1));
      assertThat(updatedChain.getTraces().get(1).getFrames(), hasSize(2));

      assertThat(result.value, differsOnlyBy(payload,
          new String[]{"data", "body", "trace_chain"}));
    }

    @Test
    public void ifMessageIsTooLargeItShouldTruncate() {
      List<Frame> frames = payloadBuilder.createFrames(2);
      Trace trace = new Trace.Builder()
          .frames(frames)
          .exception(new ExceptionInfo.Builder()
              .message(makeString("msg", 256))
              .build())
          .build();

      Payload payload = payloadBuilder.createTestPayloadSingleTrace(trace);

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(true));

      Trace updatedTrace = getBodyContentAs(Trace.class, result.value);
      assertThat(updatedTrace.getException().getMessage().length(), equalTo(255));

      assertThat(result.value, differsOnlyBy(payload,
          new String[]{"data", "body", "trace", "exception", "message"}));
    }

    @Test
    public void ifDescriptionIsSetItShouldTruncate() {
      List<Frame> frames = payloadBuilder.createFrames(2);
      Trace trace = new Trace.Builder()
          .frames(frames)
          .exception(new ExceptionInfo.Builder()
              .message("abc")
              .description("Any length is too long for this field")
              .build())
          .build();

      Payload payload = payloadBuilder.createTestPayloadSingleTrace(trace);

      TruncationStrategy.TruncationResult<Payload> result = sut.truncate(payload);
      assertThat(result.wasTruncated, equalTo(true));

      Trace updatedTrace = getBodyContentAs(Trace.class, result.value);
      assertThat(updatedTrace.getException().getDescription(), nullValue());

      assertThat(result.value, differsOnlyBy(payload,
          new String[]{"data", "body", "trace", "exception", "description"}));
    }
  }

  public static class WhenTruncatingBigTrace extends MinBodyStrategyTest {
    private TruncationStrategy.TruncationResult<Payload> result;
    private Payload original;

    @Before
    public void setUp() {
      TestPayloadBuilder payloadBuilder = new TestPayloadBuilder(1000);
      original = payloadBuilder.createTestPayloadSingleTrace(50);
      result = sut.truncate(original);
    }

    @Test
    public void itShouldTruncate() {
      assertThat(result.wasTruncated, equalTo(true));
    }

    @Test
    public void itShouldKeep1HeadAnd1TailFrames() {
      Trace trace = getBodyContentAs(Trace.class, result.value);

      assertThat(trace.getFrames(), hasSize(2));

      // The test payload builder uses frame index as line number
      assertThat(trace.getFrames().get(0).getLineNumber(), equalTo(0));
      assertThat(trace.getFrames().get(1).getLineNumber(), equalTo(49));
    }

    @Test
    public void itShouldRemoveExceptionDescriptionAndTruncateMessage() {
      Trace trace = getBodyContentAs(Trace.class, result.value);

      ExceptionInfo exception = trace.getException();
      assertThat(exception, not(nullValue()));

      assertThat(exception.getDescription(), nullValue());
      assertThat(exception.getMessage().length(), equalTo(255));
    }

    @Test
    public void itShouldOnlyModifyBody() {
      assertThat(result.value, differsOnlyBy(original, new String[] { "data", "body" }));
    }
  }

  public static class WhenTruncatingChainWithBigTraces extends MinBodyStrategyTest {
    private TruncationStrategy.TruncationResult<Payload> result;
    private Payload original;

    @Before
    public void setUp() {
      TestPayloadBuilder payloadBuilder = new TestPayloadBuilder(1000);
      original = payloadBuilder.createTestPayload(
          Arrays.asList(
              payloadBuilder.createFrames(50),
              payloadBuilder.createFrames(25),
              payloadBuilder.createFrames(10)
          )
      );
      result = sut.truncate(original);
    }

    @Test
    public void itShouldTruncate() {
      assertThat(result.wasTruncated, equalTo(true));
    }

    @Test
    public void itShouldKeep1HeadAnd1TailFrames() {
      TraceChain trace = getBodyContentAs(TraceChain.class, result.value);

      assertThat(trace.getTraces(), hasSize(3));

      // The test payload builder uses frame index as line number
      assertThat(trace.getTraces().get(0).getFrames().get(0).getLineNumber(), equalTo(0));
      assertThat(trace.getTraces().get(0).getFrames().get(1).getLineNumber(), equalTo(49));

      assertThat(trace.getTraces().get(1).getFrames().get(0).getLineNumber(), equalTo(0));
      assertThat(trace.getTraces().get(1).getFrames().get(1).getLineNumber(), equalTo(24));

      assertThat(trace.getTraces().get(2).getFrames().get(0).getLineNumber(), equalTo(0));
      assertThat(trace.getTraces().get(2).getFrames().get(1).getLineNumber(), equalTo(9));
    }

    @Test
    public void itShouldRemoveExceptionDescriptionAndTruncateMessage() {
      TraceChain chain = getBodyContentAs(TraceChain.class, result.value);

      for (Trace trace : chain.getTraces()) {
        ExceptionInfo exception = trace.getException();
        assertThat(exception, not(nullValue()));

        assertThat(exception.getDescription(), nullValue());
        assertThat(exception.getMessage().length(), equalTo(255));
      }
    }

    @Test
    public void itShouldOnlyModifyBody() {
      assertThat(result.value, differsOnlyBy(original, new String[] { "data", "body" }));
    }
  }
}
