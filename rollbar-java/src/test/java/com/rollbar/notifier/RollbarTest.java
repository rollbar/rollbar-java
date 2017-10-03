package com.rollbar.notifier;

import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RollbarTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Test
  public void shouldNotThrowExceptionWithEmptyConfig() {
    Config config = ConfigBuilder.withAccessToken("access_token").build();

    Rollbar rollbar = Rollbar.init(config);

    Throwable error = new RuntimeException("Something went wrong.");

    rollbar.log(error, null, null, Level.ERROR);
  }
}