package com.rollbar.notifier.transformer;

import com.rollbar.api.payload.data.Data;

/**
 * Transformer interface to process the data before sending it to Rollbar..
 */
public interface Transformer {

  /**
   * Transforms the incoming data into other data that will be returned.
   *
   * @param data the data to transform.
   * @return the data transformed.
   */
  Data transform(Data data);
}
