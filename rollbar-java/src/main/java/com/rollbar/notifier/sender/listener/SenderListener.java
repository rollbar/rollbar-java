package com.rollbar.notifier.sender.listener;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.result.Result;

/**
 * Sender listener to be notified by the {@link Sender}.
 */
public interface SenderListener {

  /**
   * This method is called every time the sender gets a response from Rollbar.
   *
   * @param payload the payload sent.
   * @param result the result.
   */
  void onResult(Payload payload, Result result);

  /**
   * This method is called every time there is an error in the process of sending the payloads to
   * Rollbar or processing the Rollbar response.
   *
   * @payload the payload sent.
   * @param error the error.
   */
  void onError(Payload payload, Exception error);
}
