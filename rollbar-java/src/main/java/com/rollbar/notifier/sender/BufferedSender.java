package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.queue.DiskQueue;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A buffered sender implementation.
 */
public class BufferedSender implements Sender {

  private static final int DEFAULT_BATCH_SIZE = Integer.MAX_VALUE;

  private static final long DEFAULT_FLUSH_FREQ = TimeUnit.SECONDS.toMillis(5);
  private static final long DEFAULT_INITIAL_FLUSH_DELAY = DEFAULT_FLUSH_FREQ;

  private final int batchSize;

  private Sender sender;

  private Queue<Payload> queue;

  private ScheduledExecutorService executorService;

  BufferedSender(Builder builder) {
    this(builder, Executors.newSingleThreadScheduledExecutor(new SenderThreadFactory()));
  }

  BufferedSender(Builder builder, ScheduledExecutorService executorService) {
    requireNonNull(builder.sender, "The sender can not be null");
    requireNonNull(builder.queue, "The queue can not be null");

    this.batchSize = builder.batchSize;
    this.sender = builder.sender;
    this.queue = builder.queue;

    // Schedule executor service to send events in background with a thread factory that sets the
    // thread as daemons to allow the jvm exit.
    this.executorService =  executorService;
    this.executorService.scheduleWithFixedDelay(new SendTask(batchSize, queue, sender),
        builder.initialFlushDelay, builder.flushFreq, TimeUnit.MILLISECONDS);
  }
  
  private <T> T requireNonNull(T obj, String message) {
    if (obj == null)
      throw new NullPointerException(message);
    return obj;
  }

  @Override
  public void send(Payload payload) {
    try {
      // If the queue is full it will raise an exception and it will be notified.
      queue.add(payload);
    } catch (Exception e) {
      notifyError(payload, new SenderException(e));
    }
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
    this.executorService.shutdown();
    this.sender.close();
  }

  private void notifyError(Payload payload, Exception e) {
    for (SenderListener listener : sender.getListeners()) {
      listener.onError(payload, e);
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

    public SendTask(int batchSize, Queue<Payload> queue, Sender sender) {
      this.batchSize = batchSize;
      this.queue = queue;
      this.sender = sender;
    }

    @Override
    public void run() {
      Payload payload = null;
      int numberOfSent = 0;

      try {
        while (numberOfSent < batchSize && (payload = queue.poll()) != null) {
          try {
            sender.send(payload);
          } catch (Exception e) {
            // Swallow it. The sender should notify of errors by itself and don't propagate them.
            // The result is that the payload is discarded.
          } finally {
            ++numberOfSent;
          }
        }
      } catch (Exception e) {
        // Notify senders
        for (SenderListener senderListener : sender.getListeners()) {
          senderListener.onError(payload, new SenderException(e));
        }
      }
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
}
