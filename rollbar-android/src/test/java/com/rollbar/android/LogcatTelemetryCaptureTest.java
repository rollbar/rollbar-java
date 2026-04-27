package com.rollbar.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;

import org.junit.Before;
import org.junit.Test;

public class LogcatTelemetryCaptureTest {

  private TelemetryEventTracker tracker;

  @Before
  public void setUp() {
    tracker = mock(TelemetryEventTracker.class);
  }

  @Test
  public void mapPriorityToLevel_knownPriorities() {
    assertEquals(Level.DEBUG, LogcatTelemetryCapture.mapPriorityToLevel("V"));
    assertEquals(Level.DEBUG, LogcatTelemetryCapture.mapPriorityToLevel("D"));
    assertEquals(Level.INFO, LogcatTelemetryCapture.mapPriorityToLevel("I"));
    assertEquals(Level.WARNING, LogcatTelemetryCapture.mapPriorityToLevel("W"));
    assertEquals(Level.ERROR, LogcatTelemetryCapture.mapPriorityToLevel("E"));
    assertEquals(Level.CRITICAL, LogcatTelemetryCapture.mapPriorityToLevel("F"));
  }

  @Test
  public void mapPriorityToLevel_unknownReturnsNull() {
    assertNull(LogcatTelemetryCapture.mapPriorityToLevel("X"));
    assertNull(LogcatTelemetryCapture.mapPriorityToLevel(""));
    assertNull(LogcatTelemetryCapture.mapPriorityToLevel(null));
  }

  @Test
  public void logcatPriorityFor_levels() {
    assertEquals("V", LogcatTelemetryCapture.logcatPriorityFor(Level.DEBUG));
    assertEquals("I", LogcatTelemetryCapture.logcatPriorityFor(Level.INFO));
    assertEquals("W", LogcatTelemetryCapture.logcatPriorityFor(Level.WARNING));
    assertEquals("E", LogcatTelemetryCapture.logcatPriorityFor(Level.ERROR));
    assertEquals("F", LogcatTelemetryCapture.logcatPriorityFor(Level.CRITICAL));
    assertEquals("W", LogcatTelemetryCapture.logcatPriorityFor(null));
  }

  @Test
  public void processLine_recordsWarningAtThreshold() {
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("04-20 12:34:56.789  1234  5678 W MyTag: warn message");

    verify(tracker).recordManualEventFor(eq(Level.WARNING), eq(Source.CLIENT), eq("warn message"));
  }

  @Test
  public void processLine_recordsErrorAboveThreshold() {
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("04-20 12:34:56.789  1234  5678 E MyTag: boom");

    verify(tracker).recordManualEventFor(eq(Level.ERROR), eq(Source.CLIENT), eq("boom"));
  }

  @Test
  public void processLine_skipsBelowThreshold() {
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("04-20 12:34:56.789  1234  5678 I MyTag: info message");
    capture.processLine("04-20 12:34:56.789  1234  5678 D MyTag: debug message");

    verifyNoInteractions(tracker);
  }

  @Test
  public void processLine_skipsSelfTag() {
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("04-20 12:34:56.789  1234  5678 W Rollbar: recursion risk");

    verifyNoInteractions(tracker);
  }

  @Test
  public void processLine_skipsUnparseable() {
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("--------- beginning of main");
    capture.processLine("");
    capture.processLine(null);

    verifyNoInteractions(tracker);
  }

  @Test
  public void processLine_trimsTag() {
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("04-20 12:34:56.789  1234  5678 W   MyTag  : message");

    verify(tracker).recordManualEventFor(eq(Level.WARNING), eq(Source.CLIENT), eq("message"));
  }

  @Test
  public void processLine_trackerThrow_doesNotPropagate() {
    doThrow(new RuntimeException("tracker boom"))
        .when(tracker).recordManualEventFor(any(), any(), any());
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("04-20 12:34:56.789  1234  5678 W MyTag: message");

    verify(tracker).recordManualEventFor(eq(Level.WARNING), eq(Source.CLIENT), eq("message"));
  }

  @Test
  public void processLine_messageWithColon_preserved() {
    LogcatTelemetryCapture capture = newCapture(Level.WARNING);

    capture.processLine("04-20 12:34:56.789  1234  5678 W MyTag: key: value");

    verify(tracker).recordManualEventFor(eq(Level.WARNING), eq(Source.CLIENT), eq("key: value"));
  }

  @Test
  public void processLine_defaultsToWarningWhenMinLevelIsNull() {
    LogcatTelemetryCapture capture = newCapture(null);

    capture.processLine("04-20 12:34:56.789  1234  5678 I MyTag: info");
    verify(tracker, never()).recordManualEventFor(any(), any(), any());

    capture.processLine("04-20 12:34:56.789  1234  5678 W MyTag: warn");
    verify(tracker).recordManualEventFor(eq(Level.WARNING), eq(Source.CLIENT), eq("warn"));
  }

  private LogcatTelemetryCapture newCapture(Level minLevel) {
    return new LogcatTelemetryCapture(tracker, minLevel, "Rollbar");
  }
}
