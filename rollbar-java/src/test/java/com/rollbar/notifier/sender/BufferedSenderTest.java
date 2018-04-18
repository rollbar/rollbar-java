package com.rollbar.notifier.sender;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.BufferedSender.SendTask;
import com.rollbar.notifier.sender.BufferedSender.SenderThreadFactory;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

    verify(executorService).shutdown();

    verify(sender).send(payload1);
    verify(sender).send(payload2);
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

    SendTask sut = new SendTask(2, queue, sender);

    sut.run();

    verify(sender).send(payload1);
    verify(sender).send(payload2);
  }

  @Test
  public void sendTaskShouldNotThrowErrorIfQueueFails() {
    Exception errorQueue = new RuntimeException("Error in queue");
    when(queue.poll()).thenThrow(errorQueue);

    when(sender.getListeners()).thenReturn(asList(listener));

    SendTask sut = new SendTask(1, queue, sender);

    sut.run();

    ArgumentCaptor<SenderException> argument = ArgumentCaptor.forClass(SenderException.class);
    verify(listener).onError(any(), argument.capture());

    assertThat(argument.getValue(), is(instanceOf(SenderException.class)));
    assertThat(argument.getValue().getCause(), is(errorQueue));
  }

  @Test
  public void sendTaskShouldNotThrowErrorIfSenderFails() {
    Payload payload1 = mock(Payload.class);
    Payload payload2 = mock(Payload.class);

    when(queue.poll()).thenReturn(payload1, payload2);

    doThrow(new RuntimeException("Error sending")).when(sender).send(payload1);

    SendTask sut = new SendTask(2, queue, sender);

    sut.run();

    verify(sender).send(payload1);
    verify(sender).send(payload2);
  }

}