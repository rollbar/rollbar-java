package com.rollbar.android.anr.historical;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;

import com.rollbar.android.anr.AnrDetector;
import com.rollbar.android.anr.AnrException;
import com.rollbar.android.anr.AnrListener;
import com.rollbar.android.anr.historical.stacktrace.Lines;
import com.rollbar.android.anr.historical.stacktrace.RollbarThread;
import com.rollbar.android.anr.historical.stacktrace.ThreadDumpParser;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressLint("NewApi") // Validated in the Factory
public class HistoricalAnrDetector implements AnrDetector {
  private final Logger logger;
  private final Context context;
  private final AnrListener anrListener;
  ThreadDumpParser threadDumpParser = new ThreadDumpParser(true);//todo remove isBackground

  public HistoricalAnrDetector(
      Context context,
      AnrListener anrListener,
      Logger logger
  ) {
    this.context = context;
    this.anrListener = anrListener;
    this.logger = logger;
  }

  @Override
  public void init() {
    if (anrListener == null) {
      logger.error("AnrListener is null");
      return;
    }
    Thread thread = new Thread("HistoricalAnrDetectorThread") {
      @Override
      public void run() {
        super.run();
        evaluateLastExitReasons();
      }
    };
    thread.setDaemon(true);
    thread.start();
  }


  private void evaluateLastExitReasons() {
    List<ApplicationExitInfo> applicationExitInfoList = getApplicationExitInformation();

    if (applicationExitInfoList.isEmpty()) {
      logger.debug("Empty ApplicationExitInfo List");
      return;
    }

    for (ApplicationExitInfo applicationExitInfo : applicationExitInfoList) {
      if (isNotAnr(applicationExitInfo)) {
        continue;
      }

      try {
        List<RollbarThread> threads = getThreads(applicationExitInfo);

        if (threads.isEmpty()) {
          logger.error("Error parsing ANR");
          continue;
        }

        AnrException anrException = createAnrException(threads);
        if (anrException == null) {
          logger.error("Main thread not found, skipping ANR");
        } else {
          anrListener.onAppNotResponding(anrException);
        }
      } catch (Throwable e) {
        logger.error("Can't parse ANR", e);
      }
    }
  }

  private boolean isNotAnr(ApplicationExitInfo applicationExitInfo) {
    return applicationExitInfo.getReason() != ApplicationExitInfo.REASON_ANR;
  }

  private AnrException createAnrException(List<RollbarThread> threads) {
    List<RollbarThread> rollbarThreads = new ArrayList<>();
    RollbarThread mainThread = null;
    for (RollbarThread thread: threads) {
      if (thread.isMain()) {
        mainThread = thread;
      } else {
        rollbarThreads.add(thread);
      }
    }

    if (mainThread == null) {
      return null;
    }
    return new AnrException(mainThread.toStackTraceElement(), rollbarThreads);
  }

  private List<ApplicationExitInfo> getApplicationExitInformation() {
    ActivityManager activityManager =
        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    return activityManager.getHistoricalProcessExitReasons(null, 0, 0);
  }

  private List<RollbarThread> getThreads(ApplicationExitInfo applicationExitInfo) throws IOException {
    Lines lines = getLines(applicationExitInfo);
    return threadDumpParser.parse(lines);
  }

  private Lines getLines(ApplicationExitInfo applicationExitInfo) throws IOException {
    byte[] dump = getDumpBytes(Objects.requireNonNull(applicationExitInfo.getTraceInputStream()));
    return getLines(dump);
  }

  private Lines getLines(byte[] dump) throws IOException {
    return Lines.readLines(toBufferReader(dump));
  }

  private BufferedReader toBufferReader(byte[] dump) {
    return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(dump)));
  }

  private byte[] getDumpBytes(final InputStream trace) throws IOException {
    try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

      int nRead;
      byte[] data = new byte[1024];

      while ((nRead = trace.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      return buffer.toByteArray();
    }
  }
}
