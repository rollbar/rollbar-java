package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;

/**
 * Sender interface to send the payload to Rollbar.
 */
public interface Sender {

  /**
   * Sends the payload getting notifications on the {@link SenderCallback send callback} passed.
   *
   * @param payload the payload.
   * @param callback the callback.
   */
  void send(Payload payload, SenderCallback callback);
}
