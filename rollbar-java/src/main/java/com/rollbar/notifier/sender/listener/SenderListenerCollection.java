package com.rollbar.notifier.sender.listener;

import static java.util.Collections.unmodifiableList;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * An append-only collection of listeners.
 *
 */
public class SenderListenerCollection implements SenderListener {
  private final List<SenderListener> listeners = new ArrayList<>();

  @Override
  public void onResponse(Payload payload, Response response) {
    for (SenderListener listener : listeners) {
      listener.onResponse(payload, response);
    }
  }

  @Override
  public void onError(Payload payload, Exception error) {
    for (SenderListener listener : listeners) {
      listener.onError(payload, error);
    }
  }

  public List<SenderListener> getListeners() {
    return unmodifiableList(this.listeners);
  }

  public void addListener(SenderListener listener) {
    listeners.add(listener);
  }
}
