package com.rollbar.android.anr;

public interface AnrListener {
  /**
   * Called when an ANR is detected.
   *
   * @param error The error describing the ANR.
   */
  void onAppNotResponding(AnrException error);
}
