package com.rollbar.android.anr.historical.stacktrace;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public final class Lines {
  private final ArrayList<? extends Line> mList;
  private final int mMin;
  private final int mMax;

  /** The read position inside the list. */
  public int pos;

  /** Read the whole file into a Lines object. */
  public static Lines readLines(final BufferedReader in) throws IOException {
    final ArrayList<Line> list = new ArrayList<>();

    String text;
    while ((text = in.readLine()) != null) {
      list.add(new Line(text));
    }

    return new Lines(list);
  }

  /** Construct with a list of lines. */
  public Lines(final ArrayList<Line> list) {
    this.mList = list;
    mMin = 0;
    mMax = mList.size();
  }

  /** If there are more lines to read within the current range. */
  public boolean hasNext() {
    return pos < mMax;
  }

  /**
   * Return the next line, or null if there are no more lines to read. Also returns null in the
   * error condition where pos is before the beginning.
   */
  public Line next() {
    if (pos >= mMin && pos < mMax) {
      return this.mList.get(pos++);
    } else {
      return null;
    }
  }

  /** Move the read position back by one line. */
  public void rewind() {
    pos--;
  }
}
