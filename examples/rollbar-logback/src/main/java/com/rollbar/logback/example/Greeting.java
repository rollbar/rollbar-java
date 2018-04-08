package com.rollbar.logback.example;

import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Greeting class that generates different greetings throwing exceptions every even greeting message
 * number.
 */
public class Greeting {

  private static AtomicInteger counter = new AtomicInteger(1);

  /**
   * Get the greeting message.
   * @return the greeting message.
   */
  public String greeting() {
    int current = counter.getAndAdd(1);

    if (current % 2 != 0) {
      return format("Hello Rollbar number %d", current);
    }
    throw new RuntimeException("Fatal error at greeting number: " + current);
  }
}
