package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.exception.ApiException;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.listener.SenderListenerCollection;
import com.rollbar.notifier.sender.result.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to implement {@link Sender senders.}
 * Implementations based on this class should just implement the {@link AbstractSender#doSend}
 * method and not have to deal with listener and notifications to them.
 */
public abstract class AbstractSender implements Sender {

  private final AtomicReference<Logger> loggerHolder = new AtomicReference<>();

  private final SenderListenerCollection listeners = new SenderListenerCollection();

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
    this.listeners.addListener(listener);
  }

  @Override
  public final List<SenderListener> getListeners() {
    return listeners.getListeners();
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void close(boolean wait) throws Exception {
    this.close();
  }

  private Logger logger() {
    Logger log = loggerHolder.get();
    if (log != null) {
      return log;
    }
    loggerHolder.compareAndSet(null, LoggerFactory.getLogger(this.getClass()));
    return loggerHolder.get();
  }

  private void notifyResult(Payload payload, Response response) {
    logger().debug("Payload sent uuid: {}", response.getResult().getContent());
    listeners.onResponse(payload, response);
  }

  private void notifyError(Payload payload, Exception error) {
    logger().error("Error sending the payload.", error);
    listeners.onError(payload, error);
  }

}
