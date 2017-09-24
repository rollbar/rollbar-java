package com.rollbar.notifier.fingerprint;

import com.rollbar.api.payload.data.Data;

/**
 * Interface to generate the fingerprint of the {@link Data data} to send to Rollbar.
 */
public interface FingerprintGenerator {

  /**
   * Generates the fingerprint for the data.
   *
   * @param data the data.
   * @return the fingerprint.
   */
  String from(Data data);
}
