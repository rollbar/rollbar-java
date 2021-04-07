package com.rollbar.reactivestreams.notifier.sender.http;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class ApacheRequestPublisher implements Publisher<SimpleHttpResponse> {
  private final HttpAsyncClient client;
  private final AsyncHttpRequest request;

  public ApacheRequestPublisher(HttpAsyncClient client, AsyncHttpRequest request) {
    this.client = client;
    this.request = request;
  }

  @Override
  public void subscribe(final Subscriber<? super SimpleHttpResponse> s) {
    s.onSubscribe(new RequestSubscription(s));
  }

  private class RequestSubscription implements Subscription {
    private final Subscriber<? super SimpleHttpResponse> subscriber;
    private final AtomicBoolean requested;
    private final AtomicReference<Future<SimpleHttpResponse>> task;
    private final AtomicBoolean terminal;

    public RequestSubscription(Subscriber<? super SimpleHttpResponse> subscriber) {
      this.subscriber = subscriber;
      this.requested = new AtomicBoolean(false);
      this.terminal = new AtomicBoolean(false);
      this.task = new AtomicReference<>(null);
    }

    @Override
    public void request(long n) {
      if (n > 0 && requested.compareAndSet(false, true)) {
        try {
          Future<SimpleHttpResponse> requestTask = client.execute(buildRequest(),
              SimpleResponseConsumer.create(),
              null,
              HttpClientContext.create(),
              new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse result) {
                    signal(result);
                    signalCompletion();
                }

                @Override
                public void failed(Exception ex) {
                  signal(ex);
                }

                @Override
                public void cancelled() {
                    signalCompletion();
                }
              });

          task.set(requestTask);
        } catch (Throwable t) {
          signal(t);
        }
      }
    }

    private void signalCompletion() {
      if (terminal.compareAndSet(false, true)) {
        subscriber.onComplete();
      }
    }

    private void signal(Throwable ex) {
      if (terminal.compareAndSet(false, true)) {
        subscriber.onError(ex);
      }
    }

    private void signal(SimpleHttpResponse result) {
      if (!terminal.get()) {
        subscriber.onNext(result);
      }
    }

    private SimpleRequestProducer buildRequest() {
      SimpleHttpRequest req = new SimpleHttpRequest("POST", URI.create(request.getUrl()));

      for (Map.Entry<String, String> header : request.getHeaders()) {
        req.setHeader(header.getKey(), header.getValue());
      }

      req.setBody(request.getBody(), ContentType.APPLICATION_JSON);

      return SimpleRequestProducer.create(req);
    }

    @Override
    public void cancel() {
      requested.set(true);
      Future<SimpleHttpResponse> request = this.task.get();
      if (request != null) {
        request.cancel(false);
      }
    }
  }
}
