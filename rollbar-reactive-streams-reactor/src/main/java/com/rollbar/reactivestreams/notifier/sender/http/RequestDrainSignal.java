package com.rollbar.reactivestreams.notifier.sender.http;

import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * A "future-like" class that invokes the registered callbacks every time its value reaches 0,
 * resetting itself. Useful to wait for all requests to complete.
 */
class RequestDrainSignal {
  private final AtomicInteger runningRequests;
  private final Object lock;
  private volatile Runnable thenAction;

  public RequestDrainSignal() {
    this.runningRequests = new AtomicInteger(0);
    this.thenAction = null;
    this.lock = new Object();
  }

  public void then(Runnable action) {
    synchronized (lock) {
      if (this.runningRequests.get() == 0) {
        action.run();
      } else {
        addThenAction(action);
      }
    }
  }

  public void increment() {
    runningRequests.incrementAndGet();
  }

  public void decrement() {
    if (runningRequests.decrementAndGet() == 0) {
      signal();
    }
  }

  private void signal() {
    synchronized (lock) {
      try {
        Runnable action = this.thenAction;
        if (action != null) {
          action.run();
        }
      } finally {
        this.thenAction = null;
      }
    }
  }

  private void addThenAction(Runnable action) {
    final Runnable existing = this.thenAction;
    if (existing != null) {
      this.thenAction = () -> {
        existing.run();
        action.run();
      };
    } else {
      this.thenAction = action;
    }
  }

  public void cancel() {
    this.thenAction = null;
  }

  public Publisher<Void> toPublisher() {
    return subscriber -> subscriber.onSubscribe(new Subscription() {
      @Override
      public void request(long n) {
        if (n > 0) {
          RequestDrainSignal.this.then(subscriber::onComplete);
        }
      }

      @Override
      public void cancel() {
        RequestDrainSignal.this.cancel();
      }
    });
  }
}
