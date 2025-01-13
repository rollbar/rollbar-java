package com.rollbar.android.anr.historical.stacktrace;

public final class Line {
  public int lineno;
  public String text;

  public Line(final int lineno, final String text) {
    this.lineno = lineno;
    this.text = text;
  }
}
