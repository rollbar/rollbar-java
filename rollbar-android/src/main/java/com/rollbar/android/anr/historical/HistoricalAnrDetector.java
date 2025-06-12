package com.rollbar.android.anr.historical;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;

import com.rollbar.android.anr.AnrDetector;
import com.rollbar.android.anr.AnrException;
import com.rollbar.android.anr.AnrListener;
import com.rollbar.android.anr.historical.stacktrace.Lines;
import com.rollbar.android.anr.historical.stacktrace.ThreadParser;
import com.rollbar.api.payload.data.body.RollbarThread;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

@SuppressLint("NewApi") // Validated in the Factory
public class HistoricalAnrDetector implements AnrDetector {
  private final Logger logger;
  private final Context context;
  private final AnrListener anrListener;
  private final File file;

  public HistoricalAnrDetector(
      Context context,
      AnrListener anrListener,
      File file,
      Logger logger
  ) {
    this.context = context;
    this.anrListener = anrListener;
    this.file = file;
    this.logger = logger;
  }

  @Override
  public void init() {
    if (anrListener == null) {
      logger.error("AnrListener is null");
      return;
    }

    Long lastAnrTimestamp = getLastAnrTimestamp(file);
    if (lastAnrTimestamp == null) {
      return;
    }

    Thread thread = new Thread("HistoricalAnrDetectorThread") {
      @Override
      public void run() {
        super.run();
        evaluateLastExitReasons(file, lastAnrTimestamp);
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  private Long getLastAnrTimestamp(File file) {
    if (isNotValid(file)) {
      logger.error("Can't retrieve last ANR timestamp");
      return null;
    }

    try {
      return readLastAnrTimestamp(file);
    } catch (IOException e) {
      logger.error("Error reading last ANR timestamp");
      return null;
    }
  }

  private boolean isNotValid(File file) {
    if (file != null && !file.exists()) {
      createFile(file);
    }

    return file == null || !file.exists() || !file.isFile() || !file.canRead();
  }

  private void createFile(File file) {
    try {
      file.createNewFile();
    } catch (IOException e) {
      logger.error("can't create file");
    }
  }

  private Long readLastAnrTimestamp(File file) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      if ((line = br.readLine()) != null) {
        stringBuilder.append(line);
      }
      while ((line = br.readLine()) != null) {
        stringBuilder.append("\n").append(line);
      }
    } catch (FileNotFoundException ignored) {
      return null;
    }
    String content = stringBuilder.toString();

    try {
      return (content.equals("null") || content.trim().isEmpty()) ? 0L : Long.parseLong(content.trim());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private void evaluateLastExitReasons(File file, Long lastAnrReportedTimestamp) {
    List<ApplicationExitInfo> applicationExitInfoList = getApplicationExitInformation();
    Long newestAnrTimestamp = lastAnrReportedTimestamp;

    if (applicationExitInfoList.isEmpty()) {
      logger.debug("Empty ApplicationExitInfo List");
      return;
    }

    for (ApplicationExitInfo applicationExitInfo : applicationExitInfoList) {
      if (isNotAnr(applicationExitInfo)) {
        continue;
      }

      long anrTimestamp = applicationExitInfo.getTimestamp();
      if (anrTimestamp <= lastAnrReportedTimestamp) {
        logger.warn("ANR already sent");
        continue;
      }
      try {
        List<RollbarThread> threads = getThreads(applicationExitInfo);

        if (threads.isEmpty()) {
          logger.error("Error parsing ANR");
          continue;
        }

        if (containsMainThread(threads)) {
          anrListener.onAppNotResponding(new AnrException(threads));
          if (anrTimestamp > newestAnrTimestamp) {
            newestAnrTimestamp = anrTimestamp;
            saveAnrTimestamp(file, anrTimestamp);
          }
        } else {
          logger.error("Main thread not found, skipping ANR");
        }
      } catch (Throwable e) {
        logger.error("Can't parse ANR", e);
      }
    }
  }

  private void saveAnrTimestamp(File file, long timestamp) {
    if (isNotValid(file)) {
      logger.error("Can't save last ANR timestamp");
      return;
    }

    try (final OutputStream outputStream = Files.newOutputStream(file.toPath())) {
      outputStream.write(String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
    } catch (Throwable e) {
      logger.error("Error writing the ANR marker to the disk", e);
    }
  }

  private boolean isNotAnr(ApplicationExitInfo applicationExitInfo) {
    return applicationExitInfo.getReason() != ApplicationExitInfo.REASON_ANR;
  }

  private boolean containsMainThread(List<RollbarThread> threads) {
    for (RollbarThread thread: threads) {
      if (thread.isMain()) {
        return true;
      }
    }
    return false;
  }

  private List<ApplicationExitInfo> getApplicationExitInformation() {
    ActivityManager activityManager =
        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    return activityManager.getHistoricalProcessExitReasons(null, 0, 0);
  }

  private List<RollbarThread> getThreads(ApplicationExitInfo applicationExitInfo) throws IOException {
    Lines lines = getLines(applicationExitInfo);
    ThreadParser threadParser = new ThreadParser();
    return threadParser.parse(lines);
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
