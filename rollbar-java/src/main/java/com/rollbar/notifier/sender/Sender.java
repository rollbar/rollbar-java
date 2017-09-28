package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;

/**
 * Sender interface to send the payload to Rollbar.
 */
public interface Sender {

  /**
   * Sends the payload.
   *
   * @param payload the payload.
   * @return the result.
   */
  Result send(Payload payload);

}
