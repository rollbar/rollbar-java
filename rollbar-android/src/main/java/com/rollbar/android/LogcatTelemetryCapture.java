package com.rollbar.android;

import android.util.Log;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LogcatTelemetryCapture {

  // threadtime format: "MM-dd HH:mm:ss.SSS  PID  TID L Tag: message"
  private static final Pattern LOGCAT_LINE_PATTERN = Pattern.compile(
      "^\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\d+\\s+\\d+\\s+([VDIWEF])\\s+(.+?):\\s(.*)$"
  );

  private final TelemetryEventTracker tracker;
  private final Level minimumLevel;
  private final String selfTag;
  private final ProcessFactory processFactory;

  private Thread thread;
  private Process process;
  private volatile boolean running;

  LogcatTelemetryCapture(
      TelemetryEventTracker tracker,
      Level minimumLevel,
      String selfTag
  ) {
    this(tracker, minimumLevel, selfTag, defaultProcessFactory());
  }

  LogcatTelemetryCapture(
      TelemetryEventTracker tracker,
      Level minimumLevel,
      String selfTag,
      ProcessFactory processFactory
  ) {
    this.tracker = tracker;
    this.minimumLevel = minimumLevel != null ? minimumLevel : Level.WARNING;
    this.selfTag = selfTag;
    this.processFactory = processFactory;
  }

  synchronized void start() {
    if (running) {
      return;
    }
    try {
      this.process = processFactory.start(logcatPriorityFor(this.minimumLevel));
    } catch (IOException e) {
      Log.w(Rollbar.TAG, "Failed to start logcat telemetry capture", e);
      return;
    }
    running = true;
    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        readLoop();
      }
    }, "rollbar-logcat-telemetry");
    thread.setDaemon(true);
    thread.start();
  }

  synchronized void stop() {
    if (!running) {
      return;
    }
    running = false;
    if (process != null) {
      process.destroy();
      process = null;
    }
    if (thread != null) {
      thread.interrupt();
      thread = null;
    }
  }

  private void readLoop() {
    Process currentProcess = this.process;
    if (currentProcess == null) {
      return;
    }
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(currentProcess.getInputStream(), Charset.forName("UTF-8")));
    try {
      String line;
      while (running && (line = reader.readLine()) != null) {
        processLine(line);
      }
    } catch (IOException e) {
      // Process died or was destroyed — expected on stop().
    } finally {
      try {
        reader.close();
      } catch (IOException ignored) {
      }
    }
  }

  void processLine(String line) {
    if (line == null) {
      return;
    }
    Matcher matcher = LOGCAT_LINE_PATTERN.matcher(line);
    if (!matcher.matches()) {
      return;
    }

    String priority = matcher.group(1);
    String tag = matcher.group(2).trim();
    String message = matcher.group(3);

    if (selfTag != null && selfTag.equals(tag)) {
      return;
    }

    Level level = mapPriorityToLevel(priority);
    if (level == null) {
      return;
    }
    if (level.level() < minimumLevel.level()) {
      return;
    }

    try {
      tracker.recordManualEventFor(level, Source.CLIENT, message);
    } catch (Exception e) {
      // Never let a broken tracker kill the reader thread.
    }
  }

  static Level mapPriorityToLevel(String priority) {
    if (priority == null || priority.isEmpty()) {
      return null;
    }
    switch (priority.charAt(0)) {
      case 'V':
      case 'D':
        return Level.DEBUG;
      case 'I':
        return Level.INFO;
      case 'W':
        return Level.WARNING;
      case 'E':
        return Level.ERROR;
      case 'F':
        return Level.CRITICAL;
      default:
        return null;
    }
  }

  static String logcatPriorityFor(Level level) {
    if (level == null) {
      return "W";
    }
    switch (level) {
      case DEBUG:
        return "D";
      case INFO:
        return "I";
      case WARNING:
        return "W";
      case ERROR:
        return "E";
      case CRITICAL:
        return "F";
      default:
        return "W";
    }
  }

  interface ProcessFactory {
    Process start(String priorityFilter) throws IOException;
  }

  private static ProcessFactory defaultProcessFactory() {
    return new ProcessFactory() {
      @Override
      public Process start(String priorityFilter) throws IOException {
        return new ProcessBuilder(
            "logcat", "-v", "threadtime", "*:" + priorityFilter)
            .redirectErrorStream(true)
            .start();
      }
    };
  }
}
