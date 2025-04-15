package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.notifier.truncation.TruncationStrategy.TruncationResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RollbarThreadStrategyTest {

  private TestPayloadBuilder payloadBuilder;
  private final RollbarThreadStrategy sut = new RollbarThreadStrategy();
  private static final int MAX_FRAMES = 20;

  @Before
  public void setUp() {
    payloadBuilder = new TestPayloadBuilder();
  }

  @Test
  public void ifPayloadIsNullItShouldNotTruncate() {
    TruncationResult<Payload> result = sut.truncate(null);

    verifyNoTruncation(result);
  }

  @Test
  public void ifDataIsNullItShouldNotTruncate() {
    Payload payload = new Payload.Builder(payloadBuilder.createTestPayload())
      .data(null)
      .build();

    TruncationResult<Payload> result = sut.truncate(payload);

    verifyNoTruncation(result);
  }

  @Test
  public void ifBodyIsNullItShouldNotTruncate() {
    Payload payload = payloadBuilder.createTestPayload((Body) null);

    TruncationResult<Payload> result = sut.truncate(payload);

    verifyNoTruncation(result);
  }

  @Test
  public void ifRollbarThreadsIsNullItShouldNotTruncate() {
    Payload payload = payloadBuilder.createTestPayload();

    TruncationResult<Payload> result = sut.truncate(payload);

    verifyNoTruncation(result);
  }

  @Test
  public void ifRollbarThreadsContainsFramesEqualOrLessThanMaximumItShouldNotTruncate() {
    Payload payload = payloadBuilder.createTestPayloadSingleTraceWithRollbarThreads(MAX_FRAMES);

    TruncationResult<Payload> result = sut.truncate(payload);

    verifyNoTruncation(result);
  }

  @Test
  public void ifRollbarThreadsContainsMoreFramesThanMaximumItShouldTruncate() {
    Payload payload = payloadBuilder.createTestPayloadSingleTraceWithRollbarThreads(MAX_FRAMES + 1);

    TruncationResult<Payload> result = sut.truncate(payload);

    assertTrue(result.wasTruncated);
    assertNotNull(result.value);
    assertNotEquals(payload, result.value);
  }

  private void verifyNoTruncation(TruncationResult<Payload> result) {
    assertFalse(result.wasTruncated);
    assertNull(result.value);
  }

}
