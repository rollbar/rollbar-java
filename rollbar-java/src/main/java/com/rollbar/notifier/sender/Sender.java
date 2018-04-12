package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.listener.SenderListener;

import java.io.Closeable;
import java.util.List;

/**
 * Sender interface to send the payload to Rollbar.
 */
public interface Sender extends Closeable {

  /**
   * Sends the payload getting notifications on the {@link SenderListener send callback} passed.
   *
   * @param payload the payload.
   */
  void send(Payload payload);

  /**
   * Registers a listener to get notifications of sending payloads through the
   * {@link SenderListener sender listener}.
   *
   * @param listener the listener.
   */
  void addListener(SenderListener listener);

  /**
   * Get the list of listeners registered.
   *
   * @return the list of listeners registered.
   */
  List<SenderListener> getListeners();
}
