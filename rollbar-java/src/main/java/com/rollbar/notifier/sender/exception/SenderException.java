package com.rollbar.notifier.sender.exception;

/**
 * Exception to indicate that there was a problem.
 */
public class SenderException extends RuntimeException {

  public SenderException(Exception e) {
    super(e);
  }
}
