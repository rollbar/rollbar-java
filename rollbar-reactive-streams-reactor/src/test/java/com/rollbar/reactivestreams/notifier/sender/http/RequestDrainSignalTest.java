package com.rollbar.reactivestreams.notifier.sender.http;

import com.rollbar.reactivestreams.interleave.VMLensTest;
import com.vmlens.api.AllInterleavings;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertThat;

/**
 * Test the concurrency implementation under all possible thread interleaving, using VMLens.
 */
@Category(VMLensTest.class)
public class RequestDrainSignalTest {
  private ConcurrentLinkedQueue<Throwable> threadExceptions;

  @Before
  public void setUp() {
    threadExceptions = new ConcurrentLinkedQueue<>();
  }

  @After
  public void tearDown() {
    if (!threadExceptions.isEmpty()) {
      int errorCount = threadExceptions.size();
      Throwable firstException = threadExceptions.peek();
      throw new AssertionError("There were " + errorCount +
              " unhandled exceptions in background threads. ", firstException);
    }
  }

  private Thread newThread(Runnable runnable) {
    Thread t = new Thread(runnable);
    t.setUncaughtExceptionHandler(this::collectUncaughtException);
    return t;
  }

  private void collectUncaughtException(Thread ignored, Throwable throwable) {
    this.threadExceptions.add(throwable);
  }

  @Test
  public void canCompleteAsynchronously() throws InterruptedException {
    try (AllInterleavings allInterleavings = setupInterleavings("canCompleteAsynchronously")) {
      while (allInterleavings.hasNext()) {
        AtomicInteger executionCounter = new AtomicInteger(0);
        RequestDrainSignal sut = new RequestDrainSignal();
        sut.increment();

        Thread t1 = newThread(sut::decrement);
        Thread t2 = newThread(() -> sut.then(executionCounter::incrementAndGet));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        org.junit.Assert.assertEquals(1, executionCounter.get());
      }
    }
  }

  @Test
  public void canRegisterMultipleActionsAsynchronouslyAfterSignal()
          throws InterruptedException {
    try (AllInterleavings allInterleavings = setupInterleavings(
            "canRegisterMultipleActionsAsynchronouslyAfterSignal")) {
      while (allInterleavings.hasNext()) {
        AtomicInteger result = new AtomicInteger(1);
        RequestDrainSignal sut = new RequestDrainSignal();
        sut.increment();

        // We'd like to put this on another thread but the combinatorial explosion of
        // interleavings breaks the vmlens report generator due to OOM (after a 3 minute
        // test run).
        // Running the 3 thread version can trigger some failures due to high contention,
        // since vmlens fails the test when going over a certain number of synchronization
        // actions per thread, but raising that limit makes the test pass.
        sut.decrement();

        Thread t1 =
                newThread(() -> sut.then(() -> result.accumulateAndGet(3, Integer::sum)));
        Thread t2 = newThread(() -> sut.then(result::incrementAndGet));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        org.junit.Assert.assertEquals(5, result.get());
      }
    }
  }

  @Test
  public void canRegisterMultipleActionsAsynchronouslyBeforeSignal()
          throws InterruptedException {
    try (AllInterleavings allInterleavings = setupInterleavings(
            "canRegisterMultipleActionsAsynchronouslyBeforeSignal")) {
      while (allInterleavings.hasNext()) {
        AtomicInteger result = new AtomicInteger(1);
        RequestDrainSignal sut = new RequestDrainSignal();
        sut.increment();

        Thread t1 =
                newThread(() -> sut.then(() -> result.accumulateAndGet(3, Integer::sum)));
        Thread t2 = newThread(() -> sut.then(result::incrementAndGet));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        sut.decrement();

        org.junit.Assert.assertEquals(5, result.get());
      }
    }
  }

  @Test
  public void canCancelSignalling()
          throws InterruptedException {
    try (AllInterleavings allInterleavings = setupInterleavings("canCancelSignalling")) {
      while (allInterleavings.hasNext()) {
        AtomicInteger executionCount = new AtomicInteger(0);

        RequestDrainSignal sut = new RequestDrainSignal();
        sut.increment();
        sut.then(executionCount::incrementAndGet);

        Thread t1 = newThread(sut::cancel);
        Thread t2 = newThread(sut::decrement);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // This is truly non-deterministic, since the cancellation could happen before
        // or after completion, so all we care about is that it doesn't crash, and that
        // our callback is called *at most* once.

        assertThat(executionCount.get(), Matchers.lessThanOrEqualTo(1));
      }
    }
  }

  @Test
  public void canCancelSignallingWithMultipleThenActions()
          throws InterruptedException {
    try (AllInterleavings allInterleavings =
                 setupInterleavings("canCancelSignallingWithMultipleThenActions")) {
      while (allInterleavings.hasNext()) {
        AtomicInteger executionCount = new AtomicInteger(0);

        RequestDrainSignal sut = new RequestDrainSignal();
        sut.increment();
        sut.then(executionCount::incrementAndGet);

        Thread t1 = newThread(() -> sut.then(executionCount::incrementAndGet));
        Thread t2 = newThread(sut::cancel);
        Thread t3 = newThread(sut::decrement);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // This is truly non-deterministic, since the cancellation could happen before
        // or after completion, so all we care about is that it doesn't crash, and that
        // our callback is called *at most* twice.

        assertThat(executionCount.get(), Matchers.lessThanOrEqualTo(2));
      }
    }
  }

  private AllInterleavings setupInterleavings(String name) {
    return AllInterleavings.builder(name)
            .showStatementsInExecutor()
            .maximumRuns(500000)
            .maximumSynchronizationActionsPerThread(10000)
            .build();
  }
}
