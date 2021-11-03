package com.rollbar.api.truncation;

import com.rollbar.api.annotations.Unstable;

@Unstable
public interface StringTruncatable<T> {
  T truncateStrings(int maxLength);
}
