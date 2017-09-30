package com.rollbar.notifier.sender;

import static java.util.Collections.unmodifiableList;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.result.Result;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class to implement {@link Sender senders.}
 * Implementations based on this class should just implement the {@link AbstractSender#doSend}
 * method and not have to deal with listener and notifications to them.
 */
public abstract class AbstractSender implements Sender {

  private final List<SenderListener> listeners = new ArrayList<>();

  /**
   * This method just do the send logic.
   * @param payload the payload to send.
   * @return the result.
   * @throws Exception an exception indicating that an error happen.
   */
  protected abstract Result doSend(Payload payload) throws Exception;

  @Override
  public final void send(Payload payload) {
    try {
      Result result = doSend(payload);
      notifyResult(payload, result);
    } catch (Exception e) {
      notifyError(payload, new SenderException(e));
    }
  }

  @Override
  public final void addListener(SenderListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public final List<SenderListener> getListeners() {
    return unmodifiableList(this.listeners);
  }

  @Override
  public void close() throws Exception {

  }

  private void notifyResult(Payload payload, Result result) {
    for (SenderListener listener : listeners) {
      listener.onResult(payload, result);
    }
  }

  private void notifyError(Payload payload, Exception error) {
    for (SenderListener listener : listeners) {
      listener.onError(payload, error);
    }
  }
}
