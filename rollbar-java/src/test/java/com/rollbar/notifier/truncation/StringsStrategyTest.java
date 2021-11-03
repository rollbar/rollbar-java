package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.body.Body;
import org.junit.Test;

import static com.rollbar.notifier.truncation.TruncationMatchers.hasNoStringsLongerThan;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class StringsStrategyTest {
  @Test
  public void whenTruncatingAllStringsShouldHaveSizeLessOrEqualToLimit() {
    TestPayloadBuilder builder = new TestPayloadBuilder(1025);

    Payload payload = builder.createTestPayloadSingleTrace(5);
    TruncationStrategy.TruncationResult<Payload> result = new StringsStrategy(1024)
        .truncate(payload);

    assertThat(result.wasTruncated, equalTo(true));
    assertThat(result.value, hasNoStringsLongerThan(1024));
  }

  @Test
  public void whenBodyIsNullItShouldTruncateOtherElements() {
    Payload payload = new TestPayloadBuilder(1024).createTestPayload((Body) null);

    TruncationStrategy.TruncationResult<Payload> result = new StringsStrategy(1024)
        .truncate(payload);

    assertThat(result.wasTruncated, equalTo(true));
    assertThat(result.value, hasNoStringsLongerThan(1024));
  }

  @Test
  public void whenDataIsNullItShouldNotTruncate() {
    Payload payload = new Payload.Builder(new TestPayloadBuilder().createTestPayload())
        .data(null)
        .build();

    TruncationStrategy.TruncationResult<Payload> result = new StringsStrategy(1024)
        .truncate(payload);
    assertThat(result.wasTruncated, equalTo(false));
    assertThat(result.value, nullValue());
  }
}