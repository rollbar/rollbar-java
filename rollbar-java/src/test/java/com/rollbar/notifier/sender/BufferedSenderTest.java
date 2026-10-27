package com.rollbar.notifier.sender;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.BufferedSender.SendTask;
import com.rollbar.notifier.sender.BufferedSender.SenderThreadFactory;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.sender.result.Result;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

public class BufferedSenderTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  Queue<Payload> queue;

  @Mock
  Sender sender;

  @Mock
  SenderListener listener;

  @Mock
  ScheduledExecutorService executorService;

  BufferedSender sut;

  @Before
  public void setUp() {
    sut = new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender),
        executorService);
  }

  @Test
  public void shouldEnqueuePayloads() {
    Payload payload = mock(Payload.class);

    sut.send(payload);

    verify(queue).add(payload);
  }

  @Test
  public void shouldNotifyQueueIsFull() {
    when(sender.getListeners()).thenReturn(asList(listener));

    Payload payload = mock(Payload.class);

    Exception queueFullError = new IllegalStateException("Queue full");
    when(queue.add(any())).thenThrow(queueFullError);

    sut.send(payload);

    ArgumentCaptor<SenderException> argument = ArgumentCaptor.forClass(SenderException.class);
    verify(listener).onError(eq(payload), argument.capture());

    assertThat(argument.getValue(), is(instanceOf(SenderException.class)));
    assertThat(argument.getValue().getCause(), is(queueFullError));
  }

  @Test
  public void shouldDelegateAddListener() {
    sut.addListener(listener);

    verify(sender).addListener(listener);
  }

  @Test
  public void shouldDelegateGetListeners() {
    sut.getListeners();

    verify(sender).getListeners();
  }

  @Test
  public void shouldClose() throws Exception {
    sut.close();

    verify(executorService).shutdown();
    verify(sender).close();
  }

  @Test
  public void shouldCloseWaiting() throws Exception {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    Queue<Payload> queue = new ConcurrentLinkedQueue<>();
    queue.addAll(asList(payload1, payload2));

    sut = new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender),
        executorService);

    sut.close(true);

    assertThat(queue.size(), is(0));

    InOrder inOrder = inOrder(executorService, sender);
    inOrder.verify(executorService).shutdown();
    inOrder.verify(executorService).awaitTermination(anyLong(), any(TimeUnit.class));
    inOrder.verify(sender).send(payload1);

    verify(sender).send(payload1);
    verify(sender).send(payload2);
    verify(sender).close();
  }

  @Test
  public void shouldCloseWaitingForInFlightSend() throws Exception {
    CountDownLatch sendStarted = new CountDownLatch(1);
    CountDownLatch releaseSend = new CountDownLatch(1);
    AtomicBoolean sendFinished = new AtomicBoolean(false);
    Payload payload = mock(Payload.class);

    doAnswer(invocation -> {
      sendStarted.countDown();
      releaseSend.await();
      sendFinished.set(true);
      return null;
    }).when(sender).send(payload);

    Queue<Payload> queue = new ConcurrentLinkedQueue<>();
    queue.add(payload);

    sut = new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender)
        .initialFlushDelay(0),
        Executors.newSingleThreadScheduledExecutor(new SenderThreadFactory()));

    // The scheduled run has taken the payload off the queue and is still sending it.
    assertThat(sendStarted.await(5, TimeUnit.SECONDS), is(true));
    assertThat(queue.size(), is(0));

    Thread closer = new Thread(() -> {
      try {
        sut.close(true);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    closer.start();

    closer.join(200);
    assertThat(closer.isAlive(), is(true));

    releaseSend.countDown();
    closer.join(5000);

    assertThat(closer.isAlive(), is(false));
    assertThat(sendFinished.get(), is(true));
    verify(sender).close();
  }

  @Test
  public void shouldCloseWithoutWaiting() throws Exception {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    Queue<Payload> queue = new ConcurrentLinkedQueue<>();
    queue.addAll(asList(payload1, payload2));

    sut = new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender),
        executorService);

    sut.close(false);

    assertThat(queue.size(), is(2));

    verify(executorService).shutdown();

    verify(sender, never()).send(payload1);
    verify(sender, never()).send(payload2);
    verify(sender).close();
  }

  @Test(timeout = 5000)
  public void shouldStopFlushingOnCloseWhileSendingIsSuspended() throws Exception {
    Payload payload = mock(Payload.class);
    Queue<Payload> queue = new ConcurrentLinkedQueue<>();
    queue.add(payload);

    SenderFailureStrategy strategy = mock(SenderFailureStrategy.class);
    when(strategy.isSendingSuspended()).thenReturn(true);

    sut = new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender)
        .senderFailureStrategy(strategy),
        executorService);

    sut.close(true);

    assertThat(queue.size(), is(1));
    verify(sender, never()).send(payload);
    verify(sender).close();
  }

  @Test
  public void shouldFlushForShutdownWithoutClosing() throws Exception {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    Queue<Payload> queue = new ConcurrentLinkedQueue<>();
    queue.addAll(asList(payload1, payload2));

    sut = bufferedSenderWithRealExecutor(queue);

    sut.flushForShutdown();

    assertThat(queue.size(), is(0));
    verify(sender).send(payload1);
    verify(sender).send(payload2);
    verify(sender, never()).close();

    sut.close();
  }

  @Test
  public void shouldFlushForShutdownWaitingForInFlightSend() throws Exception {
    CountDownLatch sendStarted = new CountDownLatch(1);
    CountDownLatch releaseSend = new CountDownLatch(1);
    AtomicBoolean sendFinished = new AtomicBoolean(false);
    Payload inFlight = mock(Payload.class);
    final Payload queued = mock(Payload.class);

    doAnswer(invocation -> {
      sendStarted.countDown();
      releaseSend.await();
      sendFinished.set(true);
      return null;
    }).when(sender).send(inFlight);

    Queue<Payload> queue = new ConcurrentLinkedQueue<>();
    queue.add(inFlight);

    sut = new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender)
        .initialFlushDelay(0)
        .flushFreq(TimeUnit.MINUTES.toMillis(1)),
        Executors.newSingleThreadScheduledExecutor(new SenderThreadFactory()));

    // The scheduled run has taken the payload off the queue and is still sending it.
    assertThat(sendStarted.await(5, TimeUnit.SECONDS), is(true));
    queue.add(queued);

    Thread flusher = new Thread(() -> {
      try {
        sut.flushForShutdown();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    flusher.start();

    flusher.join(200);
    assertThat(flusher.isAlive(), is(true));

    releaseSend.countDown();
    flusher.join(5000);

    assertThat(flusher.isAlive(), is(false));
    assertThat(sendFinished.get(), is(true));
    verify(sender).send(queued);
    verify(sender, never()).close();

    sut.close();
  }

  /**
   * Shutdown hooks run concurrently, so the application may keep logging after the flush. Those
   * payloads must be sent straight away, since the JVM could exit before the next scheduled run.
   */
  @Test
  public void shouldSendNewPayloadsRightAwayAfterFlushForShutdown() throws Exception {
    Payload beforeFlush = mock(Payload.class);
    final Payload afterFlush = mock(Payload.class);

    sut = bufferedSenderWithRealExecutor(new ConcurrentLinkedQueue<>());

    sut.send(beforeFlush);
    verify(sender, after(200).never()).send(beforeFlush);

    sut.flushForShutdown();
    verify(sender).send(beforeFlush);

    sut.send(afterFlush);
    verify(sender, timeout(5000)).send(afterFlush);

    sut.close();
  }

  /**
   * A retry must still wait for the next scheduled run, otherwise a network outage during
   * shutdown would burn through every attempt back to back.
   */
  @Test
  public void shouldNotRetryRightAwayAfterFlushForShutdown() throws Exception {
    final Payload failing = mock(Payload.class);
    final Payload fresh = mock(Payload.class);
    queue = new ConcurrentLinkedQueue<>();

    Response response = new Response.Builder().result(new Result.Builder().build()).build();
    setSenderMockToCallListeners(sender, response, null);

    SenderFailureStrategy strategy = mock(SenderFailureStrategy.class);
    when(strategy.getAction(any(Payload.class), eq(response)))
        .thenReturn(SenderFailureStrategy.PayloadAction.CAN_BE_RETRIED);

    // A fresh mock, so that only this sender's scheduled task gets captured below.
    executorService = mock(ScheduledExecutorService.class);
    doReturn(CompletableFuture.completedFuture(null))
        .when(executorService).submit(any(Runnable.class));

    sut = new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender)
        // One payload per run. The strategy never suspends sending, so a larger batch would keep
        // taking the retried payload straight back off the queue.
        .batchSize(1)
        .senderFailureStrategy(strategy),
        executorService);

    ArgumentCaptor<SendTask> taskCaptor = ArgumentCaptor.forClass(SendTask.class);
    verify(executorService).scheduleWithFixedDelay(
        taskCaptor.capture(), anyLong(), anyLong(), any());

    sut.flushForShutdown();

    // A scheduled run sends the payload, which fails and is queued again for retry.
    queue.add(failing);
    taskCaptor.getValue().run();

    verify(sender).send(failing);
    assertThat(queue.peek(), is(failing));
    verify(executorService, never()).execute(any(Runnable.class));

    sut.send(fresh);

    verify(executorService).execute(any(Runnable.class));
  }

  private BufferedSender bufferedSenderWithRealExecutor(Queue<Payload> queue) {
    return new BufferedSender(new BufferedSender.Builder()
        .queue(queue)
        .sender(sender)
        // Far enough out that no scheduled run gets in the way of the test.
        .initialFlushDelay(TimeUnit.MINUTES.toMillis(1))
        .flushFreq(TimeUnit.MINUTES.toMillis(1)),
        Executors.newSingleThreadScheduledExecutor(new SenderThreadFactory()));
  }

  @Test
  public void threadFactoryShouldSetNameMakeThreadsDaemons() {
    Runnable runnable = mock(Runnable.class);

    SenderThreadFactory sut = new SenderThreadFactory();
    Thread result = sut.newThread(runnable);

    assertThat(result.isDaemon(), is(true));
    assertThat(result.getName(), is("rollbar-buffered_sender"));
  }

  @Test
  public void sendTaskShouldSendPayloadInBatch() {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    when(queue.poll()).thenReturn(payload1, payload2);

    SendTask sut = new SendTask(2, queue, sender, null);

    sut.run();

    verify(sender).send(payload1);
    verify(sender).send(payload2);
  }

  @Test
  public void sendTaskShouldNotThrowErrorIfQueueFails() {
    Exception errorQueue = new RuntimeException("Error in queue");
    when(queue.poll()).thenThrow(errorQueue);

    when(sender.getListeners()).thenReturn(asList(listener));

    SendTask sut = new SendTask(1, queue, sender, null);

    sut.run();

    ArgumentCaptor<SenderException> argument = ArgumentCaptor.forClass(SenderException.class);
    verify(listener).onError(any(), argument.capture());

    assertThat(argument.getValue(), is(instanceOf(SenderException.class)));
    assertThat(argument.getValue().getCause(), is(errorQueue));
  }

  @Test
  public void sendTaskShouldNotThrowErrorIfSenderThrowsException() {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    when(queue.poll()).thenReturn(payload1, payload2);

    doThrow(new RuntimeException("Error sending")).when(sender).send(payload1);

    SendTask sut = new SendTask(2, queue, sender, null);

    sut.run();

    verify(sender).send(payload1);
    verify(sender).send(payload2);
  }

  @Test
  public void sendTaskShouldNotThrowErrorIfSenderThrowsError() {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    when(queue.poll()).thenReturn(payload1, payload2, null);

    doThrow(new StackOverflowError("Fake")).when(sender).send(eq(payload1));

    SendTask sut = new SendTask(2, queue, sender, null);

    sut.run();

    // For non-exception throwables, we catch the error and log it, but we don't continue with the
    // batch since the error might be fatal.
    verify(sender).send(eq(payload1));
    verifyNoMoreInteractions(sender);

    // If the process is still alive after that, on a second scheduled run we try the next payload.
    sut.run();

    verify(sender).send(eq(payload2));
    verifyNoMoreInteractions(sender);
  }

  @Test
  public void sendTaskIfSendingIsSuspendedItShouldNotSendPayloads() {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    when(queue.poll()).thenReturn(payload1, payload2);

    SenderFailureStrategy strategy = mock(SenderFailureStrategy.class);
    when(strategy.isSendingSuspended()).thenReturn(true);

    SendTask sut = new SendTask(2, queue, sender, strategy);

    sut.run();

    verify(sender, never()).send(any(Payload.class));
  }

  @Test
  public void ifFailureStrategyIsSetItShouldBeAppliedForEachOccurrenceSent() {
    SenderFailureStrategy strategy = mock(SenderFailureStrategy.class);

    Response response = new Response.Builder()
            .result(new Result.Builder().build()).build();
    Payload payload = mock(Payload.class);
    runFailureStrategy(strategy, null, response, payload);

    verify(strategy).getAction(eq(payload), eq(response));
    verify(strategy, never()).getAction(eq(payload), any(Exception.class));
  }

  @Test
  public void ifFailureStrategyIsSetItShouldBeAppliedForEachError() {
    SenderFailureStrategy strategy = mock(SenderFailureStrategy.class);

    Exception error = new RuntimeException("Something failed");
    Payload payload = mock(Payload.class);
    runFailureStrategy(strategy, error, null, payload);

    verify(strategy).getAction(eq(payload), eq(error));
    verify(strategy, never()).getAction(eq(payload), any(Response.class));
  }

  @Test
  public void ifFailureStrategyReturnsRetryPayloadShouldBeAddedBackToQueue() {
    SenderFailureStrategy.PayloadAction action = SenderFailureStrategy.PayloadAction.CAN_BE_RETRIED;

    // Only the first payload gets reenqueued, the second one never gets sent since sending is
    // suspended after the first one, so they are now in the queue in reverse order.
    testFailureStrategyAction(action, true, (payload1, payload2) -> {
      assertEquals(payload2, queue.poll());
      assertEquals(payload1, queue.poll());
      assertThat(queue, hasSize(0));
      verify(sender, times(1)).send(any(Payload.class));
    });
  }

  @Test
  public void ifFailureStrategyReturnsNoActionWithoutRetryQueueShouldBeConsumed() {
    SenderFailureStrategy.PayloadAction action = SenderFailureStrategy.PayloadAction.NONE;
    testFailureStrategyAction(action, false, (payload1, payload2) -> {
      assertThat(queue, hasSize(0));
      verify(sender, times(2)).send(any(Payload.class));
    });
  }

  @Test
  public void ifFailureStrategyReturnsNoRetryPayloadShouldNotBeAddedBackToQueue() {
    SenderFailureStrategy.PayloadAction action = SenderFailureStrategy.PayloadAction.NONE;

    testFailureStrategyAction(action, true, (payload1, payload2) -> {
      // Sending is suspended after first one, so only one send was attempted.
      assertThat(queue, hasSize(1));
      assertEquals(payload2, queue.peek());
      verify(sender, times(1)).send(any(Payload.class));
      verify(sender, times(1)).send(eq(payload1));
    });
  }

  @Test
  public void ifPayloadHasBeenTried30TimesItShouldNotBeAddedBackToQueue() {
    SenderFailureStrategy.PayloadAction action = SenderFailureStrategy.PayloadAction.CAN_BE_RETRIED;

    Payload payload1 = spy(new Payload.Builder().build());
    for (int j = 0; j < 29; ++j) {
      payload1.incrementSendAttemptCount();
    }

    Payload payload2 = spy(new Payload.Builder().build());
    for (int j = 0; j < 25; ++j) {
      payload2.incrementSendAttemptCount();
    }

    // Not a realistic scenario, we would never implement a strategy that marks payloads for retry
    // without suspending at least for a short period of time, but it serves this test.
    testFailureStrategyAction(action, false, payload1, payload2, (p1, p2) -> {
      assertThat(queue, hasSize(0));
      verify(sender, times(1)).send(eq(payload1));
      verify(sender, times(5)).send(eq(payload2));
    });
  }

  private void testFailureStrategyAction(SenderFailureStrategy.PayloadAction action,
                                         boolean suspendAfterSendingFirst,
                                         BiConsumer<Payload, Payload> assertions) {

    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    testFailureStrategyAction(action, suspendAfterSendingFirst, payload1, payload2, assertions);
  }

  private void testFailureStrategyAction(SenderFailureStrategy.PayloadAction action,
                                         boolean suspendAfterSendingFirst,
                                         Payload payload1,
                                         Payload payload2,
                                         BiConsumer<Payload, Payload> assertions) {
    SenderFailureStrategy strategy = mock(SenderFailureStrategy.class);

    Response response = new Response.Builder().result(new Result.Builder().build()).build();

    AtomicBoolean isSuspended = new AtomicBoolean(false);
    when(strategy.getAction(any(Payload.class), eq(response)))
            .thenAnswer((Answer<SenderFailureStrategy.PayloadAction>) invocation -> {
              if (suspendAfterSendingFirst) {
                isSuspended.set(true);
              }
              return action;
            });

    when(strategy.isSendingSuspended())
            .thenAnswer((Answer<Boolean>) invocation -> isSuspended.get());

    runFailureStrategy(strategy, null, response, payload1, payload2);

    verify(strategy).getAction(eq(payload1), eq(response));

    assertions.accept(payload1, payload2);
  }

  private void runFailureStrategy(SenderFailureStrategy strategy, Exception senderException,
                                  Response senderResponse, Payload... payloads) {
    // We need a real queue, get rid of the mock.
    queue = new ConcurrentLinkedQueue<>();

    // Instead of changing the return type of `send` and breaking backwards compatibility, we're
    // relying on the listener functionality to detect failures, so we need to ensure our mock
    // tracks and notifies listeners.
    setSenderMockToCallListeners(sender, senderResponse, senderException);

    ArgumentCaptor<SendTask> taskCaptor = ArgumentCaptor.forClass(SendTask.class);
    executorService = mock(ScheduledExecutorService.class);

    sut = new BufferedSender(new BufferedSender.Builder()
            .queue(queue)
            .sender(sender)
            .batchSize(100)
            .senderFailureStrategy(strategy),
            executorService);

    verify(executorService).scheduleWithFixedDelay(
            taskCaptor.capture(), anyLong(), anyLong(), any());

    for (int j = 0; j < payloads.length; ++j) {
      sut.send(payloads[j]);
      assertThat(queue, hasSize(j + 1));
    }

    taskCaptor.getValue().run();
  }

  private void setSenderMockToCallListeners(Sender sender, Response result, Exception exception) {
    List<SenderListener> listeners = new ArrayList<>();
    when(sender.getListeners()).thenReturn(listeners);

    doAnswer((Answer<Void>) invocation -> {
      listeners.add(invocation.getArgument(0));
      return null;
    }).when(sender).addListener(any(SenderListener.class));

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) {
        Payload payload = invocation.getArgument(0);

        for (SenderListener l : listeners) {
          if (exception != null) {
            l.onError(payload, exception);
          } else {
            l.onResponse(payload, result);
          }
        }

        return null;
      }
    }).when(sender).send(any(Payload.class));
  }
}
