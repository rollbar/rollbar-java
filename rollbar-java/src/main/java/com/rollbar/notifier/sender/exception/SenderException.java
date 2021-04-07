package com.rollbar.notifier.sender.exception;

import com.rollbar.notifier.sender.Sender;

/**
 * Exception to indicate that there was a problem related in the {@link Sender sender}.
 */
public class SenderException extends RuntimeException {
  public SenderException(Exception e) {
    super(e);
  }

  public SenderException(Throwable e) {
    super(e);
  }
}
