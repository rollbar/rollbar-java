package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.util.ObjectsUtils;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A buffered sender implementation.
 */
public class BufferedSender implements Sender {

  private static final int DEFAULT_BATCH_SIZE = Integer.MAX_VALUE;

  private static final long DEFAULT_FLUSH_FREQ = TimeUnit.SECONDS.toMillis(5);
  private static final long DEFAULT_INITIAL_FLUSH_DELAY = DEFAULT_FLUSH_FREQ;

  // We only retry payloads when we suspect the failure is caused by the network being unavailable,
  // so it makes sense to keep this high. In the case of android for example, when users enable
  // connectivity detection, sending occurrences is suspended for up to 5 minutes when the network
  // is down. So 30 retries gives us 2.5 hours of downtime before a payload is discarded.
  private static final int DEFAULT_MAX_SEND_ATTEMPT_COUNT = 30;

  private final int batchSize;

  private final int maxSendAttemptCount;

  private Sender sender;

  private Queue<Payload> queue;

  private final SenderFailureStrategy senderFailureStrategy;

  private ScheduledExecutorService executorService;

  private SendTask sendTask;

  private volatile boolean sendImmediately;

  private static Logger LOGGER = LoggerFactory.getLogger(BufferedSender.class);

  BufferedSender(Builder builder) {
    this(builder, Executors.newSingleThreadScheduledExecutor(new SenderThreadFactory()));
  }

  BufferedSender(Builder builder, ScheduledExecutorService executorService) {
    ObjectsUtils.requireNonNull(builder.sender, "The sender can not be null");
    ObjectsUtils.requireNonNull(builder.queue, "The queue can not be null");

    this.batchSize = builder.batchSize;
    this.maxSendAttemptCount = DEFAULT_MAX_SEND_ATTEMPT_COUNT;
    this.sender = builder.sender;
    this.queue = builder.queue;
    this.senderFailureStrategy = builder.senderFailureStrategy;

    if (this.senderFailureStrategy != null) {
      FailureListener failureListener = new FailureListener(builder.senderFailureStrategy);
      this.sender.addListener(failureListener);
    }

    this.sendTask = new SendTask(batchSize, queue, sender, this.senderFailureStrategy);

    // Schedule executor service to send events in background with a thread factory that sets the
    // thread as daemons to allow the jvm exit.
    this.executorService =  executorService;
    this.executorService.scheduleWithFixedDelay(this.sendTask,
        builder.initialFlushDelay, builder.flushFreq, TimeUnit.MILLISECONDS);
  }

  /**
   * Get the queue.
   *
   * @return the queue object.
   */
  public Queue<Payload> queue() {
    return queue;
  }

  /**
   * Get the queue sender.
   *
   * @return the sender object.
   */
  public Sender sender() {
    return sender;
  }

  @Override
  public void send(Payload payload) {
    if (!enqueue(payload)) {
      return;
    }

    if (sendImmediately) {
      try {
        this.executorService.execute(this.sendTask);
      } catch (RejectedExecutionException e) {
        // Closed in the meantime. The payload stays queued, as it would for any closed sender.
      }
    }
  }

  private boolean enqueue(Payload payload) {
    try {
      // If the queue is full it will raise an exception and it will be notified.
      queue.add(payload);
      return true;
    } catch (Exception e) {
      notifyError(payload, new SenderException(e));
      return false;
    }
  }

  /**
   * Sends the payloads still buffered, waiting for a send already in progress, and from then on
   * sends each new payload as soon as it arrives instead of on the next scheduled run.
   *
   * <p>
   * Meant for JVM shutdown. Unlike {@link #close(boolean) close(true)}, this leaves the sender
   * open, because shutdown hooks run concurrently: the application may still be logging errors,
   * for example from requests that fail while a web server drains, long after this returns. The
   * background thread is a daemon, so keeping it running never keeps the JVM alive.
   * </p>
   *
   * <p>
   * This method does not time out. Callers that need a bound, such as the shutdown hook, impose
   * it themselves.
   * </p>
   *
   * @throws InterruptedException if interrupted while waiting for the payloads to be sent.
   * @throws ExecutionException if sending the payloads fails.
   */
  void flushForShutdown() throws InterruptedException, ExecutionException {
    this.sendImmediately = true;

    Future<?> flush;
    try {
      // The executor runs one task at a time, so this only starts once a run already in progress
      // has finished. That run may have taken payloads off the queue that it is still sending.
      flush = this.executorService.submit(this::flushQueue);
    } catch (RejectedExecutionException e) {
      // Already closed, and closing flushes on its own.
      return;
    }
    flush.get();
  }

  @Override
  public void addListener(SenderListener listener) {
    sender.addListener(listener);
  }

  @Override
  public List<SenderListener> getListeners() {
    return sender.getListeners();
  }

  @Override
  public void close() throws IOException {
    if (this.senderFailureStrategy != null) {
      this.senderFailureStrategy.close();
    }
    this.executorService.shutdown();
    this.sender.close();
  }

  @Override
  public void close(boolean wait) throws Exception {
    if (!wait) {
      this.close();
      return;
    }

    try {
      // Stop scheduling further runs and wait for one already in progress. That run may have
      // taken payloads off the queue that it is still sending, so the queue being empty does not
      // mean everything has been sent. Callers that need a bound on this, such as the shutdown
      // hook, impose it themselves.
      this.executorService.shutdown();
      this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
      // Drain what is left, including payloads the in-flight run re-queued for retry.
      this.flushQueue();
    } finally {
      this.close();
    }
  }

  private void notifyError(Payload payload, Exception e) {
    for (SenderListener listener : sender.getListeners()) {
      listener.onError(payload, e);
    }
  }

  private void flushQueue() {
    while (queue.size() > 0) {
      if (this.sendTask.sendBatch() == 0) {
        // Nothing was taken off the queue, which is what happens while sending is suspended.
        // Stop, rather than spin for as long as the suspension lasts.
        break;
      }
    }
  }

  /**
   * Builder class for {@link BufferedSender}.
   */
  public static final class Builder {

    private int batchSize;

    private long initialFlushDelay;
    private long flushFreq;

    private Queue<Payload> queue;

    private Sender sender;

    private SenderFailureStrategy senderFailureStrategy;

    /**
     * Constructor.
     */
    public Builder() {
      this.batchSize = DEFAULT_BATCH_SIZE;
      this.initialFlushDelay = DEFAULT_INITIAL_FLUSH_DELAY;
      this.flushFreq = DEFAULT_FLUSH_FREQ;
      this.sender = null;
    }

    /**
     * The batch size for every flush.
     * @param batchSize the batch size.
     * @return the builder instance.
     */
    public Builder batchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    /**
     * The frequency to wait before the first flush of the queue in millis.
     * @param initialFlushDelay the flush frequency.
     * @return the builder instance.
     */
    public Builder initialFlushDelay(long initialFlushDelay) {
      this.initialFlushDelay = initialFlushDelay;
      return this;
    }

    /**
     * The frequency to flush the queue in millis.
     * @param flushFreq the flush frequency.
     * @return the builder instance.
     */
    public Builder flushFreq(long flushFreq) {
      this.flushFreq = flushFreq;
      return this;
    }

    /**
     * The queue.
     * @param queue the queue.
     * @return the builder instance.
     */
    public Builder queue(Queue<Payload> queue) {
      this.queue = queue;
      return this;
    }

    /**
     * The sender.
     * @param sender the sender.
     * @return the builder instance.
     */
    public Builder sender(Sender sender) {
      this.sender = sender;
      return this;
    }

    /**
     * The strategy to use when sending fails.
     * @param strategy the strategy.
     * @return the builder instance.
     */
    public Builder senderFailureStrategy(SenderFailureStrategy strategy) {
      this.senderFailureStrategy = strategy;
      return this;
    }

    /**
     * Builds the {@link BufferedSender buffered sender}.
     *
     * @return the buffered sender.
     */
    public BufferedSender build() {
      if (this.queue == null) {
        this.queue = new ConcurrentLinkedQueue<>();
      }
      if (this.sender == null) {
        this.sender = new SyncSender.Builder().build();
      }
      return new BufferedSender(this);
    }
  }

  static final class SendTask implements Runnable {
    private final int batchSize;

    private final Queue<Payload> queue;

    private final Sender sender;

    private final SenderFailureStrategy senderFailureStrategy;

    public SendTask(int batchSize, Queue<Payload> queue, Sender sender,
                    SenderFailureStrategy senderFailureStrategy) {
      this.batchSize = batchSize;
      this.queue = queue;
      this.sender = sender;
      this.senderFailureStrategy = senderFailureStrategy;
    }

    @Override
    public void run() {
      sendBatch();
    }

    /**
     * Sends up to one batch of payloads.
     *
     * @return the number of payloads taken off the queue, whether or not sending them succeeded.
     */
    int sendBatch() {
      Payload payload = null;
      int numberOfSent = 0;

      try {
        while (numberOfSent < batchSize && (payload = getItemFromQueue()) != null) {
          try {
            payload.incrementSendAttemptCount();
            sender.send(payload);
          } catch (Exception e) {
            // Swallow it. The sender should notify of errors by itself and don't propagate them.
            // The result is that the payload is discarded.
          } finally {
            ++numberOfSent;
          }
        }
      } catch (Exception e) {
        LOGGER.error("Error sending the payload.", e);
        // Notify senders
        for (SenderListener senderListener : sender.getListeners()) {
          senderListener.onError(payload, new SenderException(e));
        }
      } catch (Throwable e) {
        // Catching all throwables is not great, but if we don't catch it here, FutureTask catches
        // it a couple of stack frames above us, sets itself as failed, and the executor service
        // won't schedule our task again. FutureTask does not re-throw either, so there is no
        // difference in that regard with us catching the error.
        // See https://github.com/openjdk/jdk/blob/da75f3c4ad5bdf25167a3ed80e51f567ab3dbd01/src/java.base/share/classes/java/util/concurrent/FutureTask.java#L307
        // We put the catch clause here, and not in the loop, to avoid swallowing several
        // consecutive errors, which in case of a JVM frequently throwing OOM or other fatal
        // scenarios, gives async exceptions a chance to be thrown on a different thread that can
        // properly react to them, since our task won't run again for a few seconds.
        LOGGER.error("Fatal error sending the payload.", e);
        // This could be OOM, stack overflow, etc... So we can't call the listeners. Hopefully
        // logging still works, that's all we can do.
      }
      return numberOfSent;
    }

    private Payload getItemFromQueue() {
      if (isSuspended()) {
        return null;
      } else {
        return queue.poll();
      }
    }

    private boolean isSuspended() {
      if (senderFailureStrategy == null) {
        return false;
      }
      return senderFailureStrategy.isSendingSuspended();
    }
  }

  static final class SenderThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setName("rollbar-buffered_sender");
      thread.setDaemon(true);
      return thread;
    }
  }

  private class FailureListener implements SenderListener {
    private final SenderFailureStrategy senderFailureStrategy;

    public FailureListener(SenderFailureStrategy senderFailureStrategy) {
      ObjectsUtils.requireNonNull(senderFailureStrategy,
              "The senderFailureStrategy cannot be null");
      this.senderFailureStrategy = senderFailureStrategy;
    }

    @Override
    public void onResponse(Payload payload, Response response) {
      apply(payload, this.senderFailureStrategy.getAction(payload, response));
    }

    @Override
    public void onError(Payload payload, Exception error) {
      apply(payload, this.senderFailureStrategy.getAction(payload, error));
    }

    private void apply(Payload payload, SenderFailureStrategy.PayloadAction action) {
      switch (action) {
        case NONE:
          break;
        case CAN_BE_RETRIED:
          if (tooManySendAttempts(payload)) {
            LOGGER.warn("Discarding payload after " + payload.getSendAttemptCount() + " attempts");
          } else {
            // Not send(): after flushForShutdown() that would start a run right away, and a
            // network outage would then burn through every attempt back to back. A retry waits
            // for the next scheduled run instead, as it always has.
            enqueue(payload);
          }
          break;
        default:
          throw new IllegalArgumentException("Unknown action type " + action);
      }
    }
  }

  private boolean tooManySendAttempts(Payload payload) {
    return payload.getSendAttemptCount() >= maxSendAttemptCount;
  }
}
