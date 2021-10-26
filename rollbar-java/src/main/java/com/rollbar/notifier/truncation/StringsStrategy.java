package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;

class StringsStrategy implements TruncationStrategy {
  private final int stringLength;

  public StringsStrategy(int stringLength) {
    this.stringLength = stringLength;
  }

  @Override
  public TruncationResult<Payload> truncate(Payload payload) {
    if (payload == null || payload.getData() == null) {
      return TruncationResult.none();
    }

    return TruncationResult.truncated(payload.truncateStrings(stringLength));
  }
}
