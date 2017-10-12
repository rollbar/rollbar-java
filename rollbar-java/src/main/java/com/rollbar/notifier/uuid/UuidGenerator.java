package com.rollbar.notifier.uuid;

import com.rollbar.api.payload.data.Data;

/**
 * Interface to generate the UUID of the {@link Data data} to send to Rollbar.
 */
public interface UuidGenerator {

  /**
   * Generates the UUID for the data.
   *
   * @param data the data.
   * @return the UUID.
   */
  String from(Data data);
}
