package com.rollbar.notifier.shutdown;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.Sender;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.stubbing.Answer;

public class SenderShutdownHookTest {

  private static final long TIMEOUT = 500L;

  @Test
  public void shouldFlushTheSender() throws Exception {
    Sender sender = mock(Sender.class);

    new SenderShutdownHook(providerOf(sender), TIMEOUT).run();

    verify(sender).close(true);
  }

  @Test
  public void shouldReturnWithoutWaitingWhenTimeoutIsNotPositive() throws Exception {
    Sender sender = mock(Sender.class);

    long start = System.nanoTime();
    new SenderShutdownHook(providerOf(sender), 0L).run();
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    assertTrue("Expected not to wait, but waited " + elapsedMillis + " ms", elapsedMillis < 200L);
  }

  /**
   * The sender performs blocking network I/O without a socket timeout of its own, so the hook has
   * to give up on its own rather than let a stalled flush hold JVM shutdown open indefinitely.
   */
  @Test
  public void shouldGiveUpOnceTheTimeoutElapses() throws Exception {
    final CountDownLatch released = new CountDownLatch(1);
    Sender sender = mock(Sender.class, (Answer<Object>) invocation -> {
      if ("close".equals(invocation.getMethod().getName())) {
        // Block until the test releases us, simulating an unresponsive endpoint.
        released.await(10, TimeUnit.SECONDS);
      }
      return null;
    });

    long start = System.nanoTime();
    new SenderShutdownHook(providerOf(sender), TIMEOUT).run();
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    released.countDown();

    assertTrue("Expected to give up at ~" + TIMEOUT + " ms, but took " + elapsedMillis + " ms",
        elapsedMillis >= TIMEOUT && elapsedMillis < TIMEOUT * 6);
  }

  /**
   * A failure inside the notifier must not surface as a confusing error while the JVM is exiting.
   */
  @Test
  public void shouldNotPropagateFlushFailures() throws Exception {
    Sender sender = mock(Sender.class);
    doThrow(new IllegalStateException("network down")).when(sender).close(true);

    new SenderShutdownHook(providerOf(sender), TIMEOUT).run();

    verify(sender).close(true);
  }

  @Test
  public void shouldFlushOnADaemonThreadSoItCanNeverKeepTheJvmAlive() throws Exception {
    final AtomicBoolean daemon = new AtomicBoolean(false);
    Sender sender = mock(Sender.class, (Answer<Object>) invocation -> {
      if ("close".equals(invocation.getMethod().getName())) {
        daemon.set(Thread.currentThread().isDaemon());
      }
      return null;
    });

    new SenderShutdownHook(providerOf(sender), TIMEOUT).run();

    assertThat(daemon.get(), is(true));
  }

  /**
   * The sender is resolved when the hook runs, not when it is registered, so replacing the
   * configuration after registration flushes the sender actually in use.
   */
  @Test
  public void shouldFlushTheSenderInForceWhenTheHookRuns() throws Exception {
    Sender original = mock(Sender.class);
    final Sender replacement = mock(Sender.class);
    final AtomicBoolean replaced = new AtomicBoolean(false);

    Provider<Sender> provider = () -> replaced.get() ? replacement : original;
    Thread hook = new SenderShutdownHook(provider, TIMEOUT);

    replaced.set(true);
    hook.run();

    verify(replacement).close(true);
    verify(original, org.mockito.Mockito.never()).close(true);
  }

  @Test
  public void shouldDoNothingWhenThereIsNoSender() {
    Provider<Sender> provider = () -> null;

    new SenderShutdownHook(provider, TIMEOUT).run();
    // No exception: a notifier configured without a sender has nothing to flush.
  }

  private Provider<Sender> providerOf(final Sender sender) {
    return () -> sender;
  }
}
