package com.rollbar.reactivestreams.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.exception.ApiException;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.listener.SenderListenerCollection;
import com.rollbar.notifier.sender.result.Response;
import java.io.IOException;
import java.util.List;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SyncSenderWrapper implements Sender {

  private Logger thisLogger = LoggerFactory.getLogger(this.getClass());

  private final SenderListenerCollection listeners =
      new SenderListenerCollection();

  private final AsyncSender wrappedSender;

  SyncSenderWrapper(AsyncSender.Builder builder) {
    wrappedSender = new AsyncSender(builder);
  }

  @Override
  public void send(final Payload payload) {
    this.wrappedSender.send(payload).subscribe(new Subscriber<Response>() {
      @Override
      public void onSubscribe(Subscription s) {
        s.request(1);
      }

      @Override
      public void onNext(Response response) {
        if (response.getResult().isError()) {
          listeners.onError(payload, new SenderException(new ApiException(response)));
        } else {
          listeners.onResponse(payload, response);
        }
      }

      @Override
      public void onError(Throwable t) {
        listeners.onError(payload, new SenderException(t));
      }

      @Override
      public void onComplete() {
      }
    });
  }

  @Override
  public void addListener(SenderListener listener) {
    listeners.addListener(listener);
  }

  @Override
  public List<SenderListener> getListeners() {
    return listeners.getListeners();
  }

  @Override
  public void close(boolean wait) {
    this.wrappedSender.close(wait);
  }

  @Override
  public void close() throws IOException {
    try {
      this.close(true);
    } catch (Exception e) {
      throw new IOException("Failed to close SyncSenderWrapper", e);
    }
  }
}
