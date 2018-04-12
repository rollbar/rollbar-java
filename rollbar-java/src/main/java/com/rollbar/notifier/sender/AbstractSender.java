package com.rollbar.notifier.sender;

import static java.util.Collections.unmodifiableList;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.exception.ApiException;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.result.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to implement {@link Sender senders.}
 * Implementations based on this class should just implement the {@link AbstractSender#doSend}
 * method and not have to deal with listener and notifications to them.
 */
public abstract class AbstractSender implements Sender {

  private Logger thisLogger = LoggerFactory.getLogger(this.getClass());

  private final List<SenderListener> listeners = new ArrayList<>();

  /**
   * This method just do the send logic.
   * @param payload the payload to send.
   * @return the result.
   * @throws Exception an exception indicating that an error happen.
   */
  protected abstract Response doSend(Payload payload) throws Exception;

  @Override
  public final void send(Payload payload) {
    try {
      Response response = doSend(payload);
      if (response.getResult().isError()) {
        notifyError(payload, new SenderException(new ApiException(response)));
      } else {
        notifyResult(payload, response);
      }
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
  public void close() throws IOException {

  }

  private void notifyResult(Payload payload, Response response) {
    thisLogger.debug("Payload sent uuid: {}", response.getResult().getContent());
    for (SenderListener listener : listeners) {
      listener.onResponse(payload, response);
    }
  }

  private void notifyError(Payload payload, Exception error) {
    thisLogger.error("Error sending the payload.", error);
    for (SenderListener listener : listeners) {
      listener.onError(payload, error);
    }
  }
}
