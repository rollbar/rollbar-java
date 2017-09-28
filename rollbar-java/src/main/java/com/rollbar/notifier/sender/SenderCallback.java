package com.rollbar.notifier.sender;

import com.rollbar.notifier.sender.result.Result;

/**
 * Send handler that works as callback for being notified from the {@link Sender}.
 */
public interface SenderCallback {

  /**
   * This method is called every time the sender gets a response from Rollbar.
   *
   * @param result the result.
   */
  void onResult(Result result);

  /**
   * This method is called every time there is an error in the process of sending the payloads to
   * Rollbar or processing the Rollbar response.
   *
   * @param error the error.
   */
  void onError(Exception error);
}
