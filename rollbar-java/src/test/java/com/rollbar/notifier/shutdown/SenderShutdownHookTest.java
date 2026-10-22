package com.rollbar.notifier.shutdown;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.Sender;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class SenderShutdownHookTest {

  private static final long TIMEOUT = 500L;

  private static final String FLUSH_THREAD_NAME = "rollbar-shutdown-flush";

  private final List<Throwable> uncaughtOnFlushThread = new CopyOnWriteArrayList<>();

  private UncaughtExceptionHandler previousHandler;

  /**
   * The flush runs on its own thread, so anything it throws can never reach the thread that called
   * {@link SenderShutdownHook#run()}. It would end up in the uncaught exception handler instead,
   * so that is where these tests look for it.
   */
  @Before
  public void setUp() {
    previousHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
      if (FLUSH_THREAD_NAME.equals(thread.getName())) {
        uncaughtOnFlushThread.add(error);
      } else if (previousHandler != null) {
        previousHandler.uncaughtException(thread, error);
      }
    });
  }

  @After
  public void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(previousHandler);
  }

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
    assertThat(uncaughtOnFlushThread, is(empty()));
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

  /**
   * A notifier configured without a sender has nothing to flush, and that must not fail.
   */
  @Test
  public void shouldDoNothingWhenThereIsNoSender() {
    final AtomicBoolean consulted = new AtomicBoolean(false);
    Provider<Sender> provider = () -> {
      consulted.set(true);
      return null;
    };

    new SenderShutdownHook(provider, TIMEOUT).run();

    assertThat(consulted.get(), is(true));
    assertThat(uncaughtOnFlushThread, is(empty()));
  }

  private Provider<Sender> providerOf(final Sender sender) {
    return () -> sender;
  }
}
