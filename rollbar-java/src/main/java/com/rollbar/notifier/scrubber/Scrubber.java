package com.rollbar.notifier.scrubber;

import com.rollbar.api.payload.data.Data;

/**
 * 
 */
public interface Scrubber {

  /**
   * scrubs the incoming data into other data that will be returned.
   *
   * @param data the data to transform.
   * @return the data transformed.
   */
  Data scrub(Data data);
}