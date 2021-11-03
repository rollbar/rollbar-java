package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;

interface TruncationStrategy {
  /**
   * Truncate the payload.
   * @param payload The payload to be truncated.
   * @return A TruncationResult instance.
   */
  TruncationResult<Payload> truncate(Payload payload);

  class TruncationResult<T> {
    /**
     * True if the value was truncated.
     */
    public final boolean wasTruncated;
    /**
     * If the value was truncated, this will hold the truncated value. Otherwise this will
     * be null.
     */
    public final T value;

    private TruncationResult(boolean wasTruncated, T result) {
      this.wasTruncated = wasTruncated;
      this.value = result;
    }

    public static <T> TruncationResult<T> none() {
      return new TruncationResult<>(false, null);
    }

    public static <T> TruncationResult<T> truncated(T result) {
      return new TruncationResult<>(true, result);
    }
  }
}
