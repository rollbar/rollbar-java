package com.rollbar.android.anr.historical.stacktrace;

public final class Line {
  private String text;

  public Line(final String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
