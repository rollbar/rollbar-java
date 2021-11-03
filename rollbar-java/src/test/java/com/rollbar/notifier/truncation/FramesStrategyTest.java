package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.body.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.rollbar.notifier.PayloadTestHelper.getBodyContentAs;
import static com.rollbar.notifier.truncation.TruncationMatchers.differsOnlyBy;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public abstract class FramesStrategyTest {
  private static final int HEAD_FRAMES = 10;
  private static final int TAIL_FRAMES = 10;
  private static final int MAX_FRAMES = HEAD_FRAMES + TAIL_FRAMES;

  protected TestPayloadBuilder builder;

  @Before
  public void setUp() {
     this.builder = new TestPayloadBuilder();
  }

  public static class TraceChainTest extends FramesStrategyTest {
    @Test
    public void whenFrameCountIsLessThan20ItShouldNotTruncate() {
      testNoTruncation(MAX_FRAMES - 1, MAX_FRAMES / 2);
    }

    @Test
    public void whenFrameCountIs20ItShouldNotTruncate() {
      testNoTruncation(MAX_FRAMES, MAX_FRAMES, MAX_FRAMES, MAX_FRAMES);
    }

    @Test
    public void whenTruncating21FramesStartChainItShouldLeave10HeadAnd10TailFrames() {
      testChainTruncation(
          MAX_FRAMES + 1,
          MAX_FRAMES,
          MAX_FRAMES - 1
      );
    }

    @Test
    public void whenTruncating21FramesMidChainItShouldLeave10HeadAnd10TailFrames() {
      testChainTruncation(
          MAX_FRAMES - 1,
          MAX_FRAMES,
          MAX_FRAMES + 1,
          MAX_FRAMES / 3
      );
    }

    @Test
    public void whenTruncating21FramesEndChainItShouldLeave10HeadAnd10TailFrames() {
      testChainTruncation(
          MAX_FRAMES - 1,
          MAX_FRAMES,
          MAX_FRAMES,
          MAX_FRAMES / 2,
          MAX_FRAMES + 1);
    }

    @Test
    public void whenTruncating21FramesInMultipleTracesItShouldLeave10HeadAnd10TailFrames() {
      testChainTruncation(
          MAX_FRAMES + 15,
          MAX_FRAMES + 1,
          MAX_FRAMES * 20,
          MAX_FRAMES * 30
      );
    }
  }

  public static class TraceTest extends FramesStrategyTest {
    @Test
    public void whenFrameCountIsLessThan20ItShouldNotTruncate() {
      testNoTruncation(MAX_FRAMES - 1);
    }

    @Test
    public void whenFrameCountIs20ItShouldNotTruncate() {
      testNoTruncation(MAX_FRAMES);
    }

    @Test
    public void whenTruncating21FramesItShouldLeave10HeadAnd10TailFrames() {
      testTraceTruncation(MAX_FRAMES + 1);
    }

    @Test
    public void whenTruncatingManyFramesItShouldLeave10HeadAnd10TailFrames() {
      testTraceTruncation(MAX_FRAMES * 30);
    }
  }

  public static class GeneralTests extends FramesStrategyTest {
    @Test
    public void whenBodyIsMessageItShouldNotTruncate() {
      Payload payload = builder.createTestPayload(
          new Body.Builder()
          .bodyContent(new Message.Builder().body("A message").build())
          .build()
      );

      TruncationStrategy.TruncationResult<Payload> result = new FramesStrategy().truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }

    @Test
    public void whenBodyIsNullItShouldNotTruncate() {
      Payload payload = builder.createTestPayload((Body) null);

      TruncationStrategy.TruncationResult<Payload> result = new FramesStrategy().truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }

    @Test
    public void whenDataIsNullItShouldNotTruncate() {
      Payload payload = new Payload.Builder(builder.createTestPayload())
          .data(null)
          .build();

      TruncationStrategy.TruncationResult<Payload> result = new FramesStrategy().truncate(payload);
      assertThat(result.wasTruncated, equalTo(false));
      assertThat(result.value, nullValue());
    }


    @Test
    public void ifTraceContainsNullFrameItShouldStillTruncate() {
      // The body content itself cannot be null (the builders for each subclass will fail to build),
      // but the frames can be null.
      List<Frame> frames = new ArrayList<>();
      frames.add(new Frame.Builder().method("test").filename("test").build());
      frames.add(null);
      for (int j = 0; j < 50; ++j) {
        frames.add(new Frame.Builder().method("test" + j).filename("file" + j).build());
      }

      Payload payload = builder.createTestPayload(Collections.singletonList(frames));

      TruncationStrategy.TruncationResult<Payload> result = new FramesStrategy().truncate(payload);
      assertThat(result.wasTruncated, equalTo(true));

      Trace trace = getBodyContentAs(Trace.class, result.value);

      assertThat(trace.getFrames(), hasSize(20));
      assertThat(trace.getFrames().get(0).getMethod(), equalTo("test"));
      assertThat(trace.getFrames().get(19).getMethod(), equalTo("test49"));
    }
  }

  protected void testNoTruncation(int... frameCounts) {
    Payload original = createPayloadFromFrameCounts(frameCounts);

    TruncationStrategy.TruncationResult<Payload> result = new FramesStrategy().truncate(original);
    assertThat(result.wasTruncated, equalTo(false));
    assertThat(result.value, nullValue());
  }

  protected void testChainTruncation(int... frameCounts) {
    Payload original = createPayloadFromFrameCounts(frameCounts);

    TruncationStrategy.TruncationResult<Payload> result = new FramesStrategy().truncate(original);
    assertThat(result.wasTruncated, equalTo(true));

    TraceChain chain = getBodyContentAs(TraceChain.class, result.value);
    assertThat(chain.getTraces(), hasSize(frameCounts.length));

    for (int j = 0; j < frameCounts.length; ++j) {
      if (frameCounts[j] <= MAX_FRAMES) {
        assertThat(chain.getTraces().get(j).getFrames(), hasSize(frameCounts[j]));
      } else {
        verifyTruncatedTrace(frameCounts[j], chain.getTraces().get(j));
      }
    }

    assertThat(result.value, differsOnlyBy(original, new String[]{"data", "body", "trace_chain"}));
  }

  protected void testTraceTruncation(int frameCount) {
    Payload original = builder.createTestPayloadSingleTrace(frameCount);
    TruncationStrategy.TruncationResult<Payload> result = new FramesStrategy().truncate(original);
    assertThat(result.wasTruncated, equalTo(true));

    verifyTruncatedTrace(frameCount, getBodyContentAs(Trace.class, result.value));

    // Our payload class is immutable, so a new one had to be built during truncation. We should
    // verify that only the frames were modified.
    assertThat(result.value, differsOnlyBy(original,
        new String[]{"data", "body", "trace", "frames"}));
  }

  protected void verifyTruncatedTrace(int originalFrameCount, Trace actual) {
    assertThat(actual.getFrames(), hasSize(MAX_FRAMES));

    // The test payload uses the frame index as line number so we use it to check which frames were
    // kept.

    // Check head
    for (int j = 0; j < HEAD_FRAMES; ++j) {
      assertThat(actual.getFrames().get(j).getLineNumber(), equalTo(j));
    }

    // Check tail
    for (int j = 0; j < TAIL_FRAMES; ++j) {
      int frameIndex = j + HEAD_FRAMES;
      int expectedLine = originalFrameCount - TAIL_FRAMES + j;
      assertThat(actual.getFrames().get(frameIndex).getLineNumber(), equalTo(expectedLine));
    }
  }

  private Payload createPayloadFromFrameCounts(int[] frameCounts) {
    List<List<Frame>> traces = Arrays.stream(frameCounts).mapToObj(builder::createFrames)
        .collect(Collectors.toList());

    return new TestPayloadBuilder().createTestPayload(traces);
  }
}
