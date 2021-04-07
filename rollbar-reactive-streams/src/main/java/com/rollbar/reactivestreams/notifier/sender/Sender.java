package com.rollbar.reactivestreams.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Response;
import org.reactivestreams.Publisher;

/**
 * Sender interface to asynchronously send the payload to Rollbar.
 */
public interface Sender extends AutoCloseable {
  /**
   * Sends the payload.
   *
   * @param payload the payload.
   * @return A {@link Publisher} that will execute the operation once a subscriber requests it.
   */
  Publisher<Response> send(Payload payload);

  void close(boolean wait);
}
