package com.rollbar.notifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.sender.Sender;

import org.junit.Test;

/**
 * Covers the registration side of the shutdown hook. The flushing behaviour itself is covered by
 * {@link com.rollbar.notifier.shutdown.SenderShutdownHookTest}.
 */
public class RollbarShutdownHookTest {

  @Test
  public void shouldRegisterTheShutdownHookByDefault() throws Exception {
    Rollbar rollbar = new Rollbar(configWith(mock(Sender.class), true));
    try {
      assertThat(rollbar.shutdownHook(), is(notNullValue()));
    } finally {
      rollbar.close(false);
    }
  }

  @Test
  public void shouldNotRegisterTheShutdownHookWhenDisabled() throws Exception {
    Rollbar rollbar = new Rollbar(configWith(mock(Sender.class), false));
    try {
      assertThat(rollbar.shutdownHook(), is(nullValue()));
    } finally {
      rollbar.close(false);
    }
  }

  @Test
  public void shouldRemoveTheShutdownHookOnClose() throws Exception {
    Rollbar rollbar = new Rollbar(configWith(mock(Sender.class), true));
    Thread hook = rollbar.shutdownHook();
    assertThat(hook, is(notNullValue()));

    rollbar.close(false);

    assertThat(rollbar.shutdownHook(), is(nullValue()));
    // Idempotent: the hook is really gone from the JVM registry, not just from our field.
    assertThat(Runtime.getRuntime().removeShutdownHook(hook), is(false));
  }

  /**
   * Closing explicitly must flush exactly once. If the hook were left registered it would run a
   * second flush against an already closed sender while the JVM was exiting.
   */
  @Test
  public void shouldFlushOnlyOnceWhenClosedExplicitly() throws Exception {
    Sender sender = mock(Sender.class);
    Rollbar rollbar = new Rollbar(configWith(sender, true));

    rollbar.close(true);

    verify(sender).close(true);
    assertThat(rollbar.shutdownHook(), is(nullValue()));
  }

  @Test
  public void shouldDefaultToFlushingOnShutdownWithATwoSecondBound() {
    Config config = ConfigBuilder.withAccessToken("token").build();

    assertThat(config.flushOnShutdown(), is(true));
    assertThat(config.shutdownTimeoutMillis(), is(2000L));
  }

  @Test
  public void shouldCarryTheSettingsOverWhenRebuildingFromAnExistingConfig() {
    Config original = ConfigBuilder.withAccessToken("token")
        .flushOnShutdown(false)
        .shutdownTimeoutMillis(750L)
        .build();

    Config copy = ConfigBuilder.withConfig(original).build();

    assertThat(copy.flushOnShutdown(), is(false));
    assertThat(copy.shutdownTimeoutMillis(), is(750L));
  }

  private Config configWith(Sender sender, boolean flushOnShutdown) {
    return ConfigBuilder.withAccessToken("token")
        .sender(sender)
        .handleUncaughtErrors(false)
        .flushOnShutdown(flushOnShutdown)
        .shutdownTimeoutMillis(100L)
        .build();
  }
}
