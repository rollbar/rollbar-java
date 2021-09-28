package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Response;

import java.io.Closeable;

/**
 * Interface for strategy classes to deal with sending failures.
 * <p>
 * A SenderFailureStrategy determines the action to take when a payload cannot be sent, for example
 * to suspend the sending of occurrences while the network is down.
 * </p>
 */
public interface SenderFailureStrategy extends Closeable {
  /**
   * Get the action to perform after the payload has been sent.
   *
   * @param payload The payload that was sent
   * @param response The server's response
   * @return Non-null Action instance
   */
  PayloadAction getAction(Payload payload, Response response);

  /**
   * Get the action to perform after the error.
   *
   * @param payload The payload that the sender attempted to send
   * @param error The error that occurred
   * @return Non-null Action instance
   */
  PayloadAction getAction(Payload payload, Exception error);

  /**
   * Is sending of occurrences currently suspended.
   *
   * @return true if sending occurrences is currently suspended
   */
  boolean isSendingSuspended();

  enum PayloadAction {
    /**
     * No further action on the payload is necessary.
     */
    NONE,
    /**
     * <p>
     *     Sending the occurrence wasn't successful for a reason that warrants a retry (eg. a
     *     network connection wasn't available at the time.)
     * </p>
     * <p>
     *     Note the sender might still choose not to retry for other reasons, eg. there have been
     *     too many attempts to send the same payload.
     * </p>
     */
    CAN_BE_RETRIED
  }
}
