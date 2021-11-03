package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.sender.json.JsonSerializerImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.rollbar.notifier.truncation.TruncationMatchers.hasNoStringsLongerThan;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class PayloadTruncatorTest {
  private JsonSerializer serializer;
  private PayloadTruncator truncator;
  private int maxPayloadSizeBytes;
  private TestPayloadBuilder builder;

  @Before
  public void setUp() {
    serializer = new JsonSerializerImpl();
    truncator = new PayloadTruncator(serializer);
    maxPayloadSizeBytes = 1024 * 512;
    builder = new TestPayloadBuilder();
  }

  @Test
  public void whenPayloadIsWithinLimitItShouldNotBeTruncated() {
    Payload payload = builder.createTestPayload();

    String original = serializer.toJson(payload);

    // Ensure test is valid
    assertThat(PayloadTruncator.sizeInBytes(original), lessThanOrEqualTo(maxPayloadSizeBytes));

    Payload result = truncator.truncate(payload, maxPayloadSizeBytes).getPayload();
    String updated = serializer.toJson(result);

    assertThat(TestString.of(updated), equalTo(TestString.of(original)));
  }

  @Test
  public void whenPayloadIsLargerThanLimitDueToTraceItShouldBeTruncated() {
    Payload payload = builder.createTestPayloadSingleTrace(5000);

    String original = serializer.toJson(payload);

    // Ensure the test is valid
    assertThat(PayloadTruncator.sizeInBytes(original), greaterThan(maxPayloadSizeBytes));

    Payload result = truncator.truncate(payload, maxPayloadSizeBytes).getPayload();
    String updated = serializer.toJson(result);

    assertThat(TestString.of(updated), not(equalTo(TestString.of(original))));

    assertThat(PayloadTruncator.sizeInBytes(updated), lessThanOrEqualTo(maxPayloadSizeBytes));
  }

  @Test
  public void whenPayloadIsLargerThanLimitDueToTraceChainItShouldBeTruncated() {
    Payload payload = builder.createTestPayload(
        Arrays.asList(
            builder.createFrames(2500),
            builder.createFrames(2500)
        )
    );

    String original = serializer.toJson(payload);

    // Ensure the test is valid
    assertThat(PayloadTruncator.sizeInBytes(original), greaterThan(maxPayloadSizeBytes));

    Payload result = truncator.truncate(payload, maxPayloadSizeBytes).getPayload();
    String updated = serializer.toJson(result);

    assertThat(TestString.of(updated), not(equalTo(TestString.of(original))));

    assertThat(PayloadTruncator.sizeInBytes(updated), lessThanOrEqualTo(maxPayloadSizeBytes));
  }

  @Test
  public void whenPayloadIsLargerThanLimitDueToLargeStringsItShouldBeTruncated() {
    builder = new TestPayloadBuilder(30000);
    Payload payload = builder.createTestPayloadSingleTrace(5);

    String original = serializer.toJson(payload);

    // Ensure the test is valid
    assertThat(PayloadTruncator.sizeInBytes(original), greaterThan(maxPayloadSizeBytes));

    Payload result = truncator.truncate(payload, maxPayloadSizeBytes).getPayload();
    String updated = serializer.toJson(result);

    assertThat(TestString.of(updated), not(equalTo(TestString.of(original))));

    assertThat(PayloadTruncator.sizeInBytes(updated), lessThanOrEqualTo(maxPayloadSizeBytes));
    assertThat(result, hasNoStringsLongerThan(1024));
  }

  @Test
  public void whenPayloadIsTooLargeDueToManyStringsItShouldApplyMoreAggressiveTruncation() {
    // Adjust the number of custom fields as necessary if the payload structure changes. The goal
    // is to produce a payload larger than the maximum size, that can be brought down below the
    // limit by truncating all strings to 256 characters.
    builder = new TestPayloadBuilder(300, 1800);
    Payload payload = builder.createTestPayloadSingleTrace(5);

    String original = serializer.toJson(payload);

    // Ensure the test is valid
    assertThat(PayloadTruncator.sizeInBytes(original), greaterThan(maxPayloadSizeBytes));

    Payload result = truncator.truncate(payload, maxPayloadSizeBytes).getPayload();
    String updated = serializer.toJson(result);

    assertThat(TestString.of(updated), not(equalTo(TestString.of(original))));

    assertThat(PayloadTruncator.sizeInBytes(updated), lessThanOrEqualTo(maxPayloadSizeBytes));
    assertThat(result, hasNoStringsLongerThan(256));
  }
}
