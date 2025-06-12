package com.rollbar.android.anr.historical;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;

import com.rollbar.android.anr.AnrListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HistoricalAnrDetectorTest {

  @Mock
  private ApplicationExitInfo applicationExitInfo;

  @Mock
  private Context context;

  private File file;

  @Mock
  private AnrListener anrListener;

  @Mock
  private Logger logger;

  @Mock
  private ActivityManager activityManager;

  private HistoricalAnrDetector historicalAnrDetector;

  private final static long ANR_TIMESTAMP = 10L;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    createTemporalFile();
    historicalAnrDetector = new HistoricalAnrDetector(context, anrListener, file, logger);
  }

  @Test
  public void shouldNotDetectAnrWhenAnrListenerIsNull() throws InterruptedException {
    givenAnActivityManagerWithoutExitInfo();
    historicalAnrDetector = new HistoricalAnrDetector(context, null, file, logger);

    whenDetectorIsExecuted();

    thenTheListenerIsNeverCalled();
    thenErrorLogMustSay("AnrListener is null");
  }

  @Test
  public void shouldNotDetectAnrWhenApplicationExitInfoIsEmpty() throws InterruptedException {
    givenAnActivityManagerWithoutExitInfo();

    whenDetectorIsExecuted();

    thenTheListenerIsNeverCalled();
    thenDebugLogMustSay("Empty ApplicationExitInfo List");
  }

  @Test
  public void shouldNotDetectAnrWhenMainThreadIsNotParsed() throws InterruptedException, IOException {
    givenAnActivityManagerWithAnAnr(anrWithoutMainThread());

    whenDetectorIsExecuted();

    thenTheListenerIsNeverCalled();
    thenErrorLogMustSay("Main thread not found, skipping ANR");
  }

  @Test
  public void shouldDoNothingIfFileForAnrTimeStampsIsNull() throws InterruptedException, IOException {
    givenAnActivityManagerWithAnAnr(anrWithoutMainThread());
    historicalAnrDetector = new HistoricalAnrDetector(context, anrListener, null, logger);

    whenDetectorIsExecuted();

    thenTheListenerIsNeverCalled();
    thenErrorLogMustSay("Can't retrieve last ANR timestamp");
  }

  @Test
  public void shouldNotSendAnrIfItHasAlreadyBeenSent() throws InterruptedException, IOException {
    givenAnActivityManagerWithAnAnr(anr());
    givenAnAlreadySentAnr();

    whenDetectorIsExecuted();

    thenWarningLogMustSay("ANR already sent");
  }

  @Test
  public void shouldDetectAnr() throws InterruptedException, IOException {
    givenAnActivityManagerWithAnAnr(anr());

    whenDetectorIsExecuted();

    thenTheListenerIsCalled();
  }

  private void whenDetectorIsExecuted() throws InterruptedException {
    historicalAnrDetector.init();
    waitForDetectorToRun();
  }

  private void givenAnActivityManagerWithAnAnr(ByteArrayInputStream anr) throws IOException {
    setAnr(anr);
    setActivityManagerService();

    List<ApplicationExitInfo> list = new ArrayList<>();
    list.add(applicationExitInfo);
    setExitReason(list);
  }

  private void givenAnActivityManagerWithoutExitInfo() {
    setActivityManagerService();
    setExitReason(new ArrayList<>());
  }

  private void setActivityManagerService() {
    when(context.getSystemService(eq(Context.ACTIVITY_SERVICE))).thenReturn(activityManager);
  }

  private void setAnr(ByteArrayInputStream anr) throws IOException {
    givenAnAnrNotSent();
    when(applicationExitInfo.getReason()).thenReturn(ApplicationExitInfo.REASON_ANR);
    when(applicationExitInfo.getTraceInputStream()).thenReturn(anr);
  }

  private void givenAnAnrNotSent() {
    when(applicationExitInfo.getTimestamp()).thenReturn(ANR_TIMESTAMP + 1);
  }

  private void givenAnAlreadySentAnr() {
    when(applicationExitInfo.getTimestamp()).thenReturn(ANR_TIMESTAMP);
  }

  private void setExitReason(List<ApplicationExitInfo> applicationExitInfos) {
    when(activityManager.getHistoricalProcessExitReasons(eq(null), eq(0), eq(0))).thenReturn(applicationExitInfos);
  }

  private ByteArrayInputStream anr() {
    String string = "\"main\" prio=5 tid=1 Sleeping\n" +
        "| group=\"main\" sCount=1 ucsCount=0 flags=1 obj=0x72273478 self=0xb4000077e811ff50\n" +
        "| sysTid=20408 nice=-10 cgrp=top-app sched=0/0 handle=0x79e97864f8\n" +
        "| state=S schedstat=( 856373236 2319887008 1428 ) utm=74 stm=10 core=0 HZ=100\n" +
        "| stack=0x7fd2fc2000-0x7fd2fc4000 stackSize=8188KB\n" +
        "| held mutexes=" +
        "at java.lang.Thread.sleep(Native method)" +
        "- sleeping on <0x0c0f663b> (a java.lang.Object)\n" +
        "at java.lang.Thread.sleep(Thread.java:450)\n" +
        "- locked <0x0c0f663b> (a java.lang.Object)\n" +
        "at java.lang.Thread.sleep(Thread.java:355)\n" +
        "at com.rollbar.example.android.MainActivity.clickAction(MainActivity.java:77)\n" +
        "at com.rollbar.example.android.MainActivity.access$000(MainActivity.java:14)\n" +
        "at com.rollbar.example.android.MainActivity$1$1.onClick(MainActivity.java:34)\n" +
        "at android.support.design.widget.Snackbar$1.onClick(Snackbar.java:255)\n" +
        "at android.view.View.performClick(View.java:7659)\n" +
        "at android.view.View.performClickInternal(View.java:7636)\n" +
        "at android.view.View.-$$Nest$mperformClickInternal(unavailable:0)\n" +
        "at android.view.View$PerformClick.run(View.java:30156)\n" +
        "at android.os.Handler.handleCallback(Handler.java:958)\n" +
        "at android.os.Handler.dispatchMessage(Handler.java:99)\n" +
        "at android.os.Looper.loopOnce(Looper.java:205)\n" +
        "at android.os.Looper.loop(Looper.java:294)\n" +
        "at android.app.ActivityThread.main(ActivityThread.java:8177)\n" +
        "at java.lang.reflect.Method.invoke(Native method)\n" +
        "at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)\n" +
        "at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)\n" +
        "\"OkHttp ConnectionPool\" daemon prio=5 tid=3 TimedWaiting\n" +
        "| group=\"main\" sCount=1 ucsCount=0 flags=1 obj=0x12c4ddc0 self=0xb4000077e8175220\n" +
        "| sysTid=20482 nice=0 cgrp=top-app sched=0/0 handle=0x76fd901cb0\n" +
        "| state=S schedstat=( 598626 7237000 4 ) utm=0 stm=0 core=0 HZ=100\n" +
        "| stack=0x76fd7fe000-0x76fd800000 stackSize=1039KB\n" +
        "| held mutexes=\n" +
        "at java.lang.Object.wait(Native method)\n" +
        "- waiting on <0x06842b17> (a com.android.okhttp.ConnectionPool)\n" +
        "at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)\n" +
        "- locked <0x06842b17> (a com.android.okhttp.ConnectionPool)\n" +
        "at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)\n" +
        "at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:644)\n" +
        "at java.lang.Thread.run(Thread.java:1012)\n";
    return new ByteArrayInputStream(string.getBytes());
  }

  private ByteArrayInputStream anrWithoutMainThread() {
    String string = "\"OkHttp ConnectionPool\" daemon prio=5 tid=3 TimedWaiting\n" +
        "| group=\"main\" sCount=1 ucsCount=0 flags=1 obj=0x12c4ddc0 self=0xb4000077e8175220\n" +
        "| sysTid=20482 nice=0 cgrp=top-app sched=0/0 handle=0x76fd901cb0\n" +
        "| state=S schedstat=( 598626 7237000 4 ) utm=0 stm=0 core=0 HZ=100\n" +
        "| stack=0x76fd7fe000-0x76fd800000 stackSize=1039KB\n" +
        "| held mutexes=\n" +
        "at java.lang.Object.wait(Native method)\n" +
        "- waiting on <0x06842b17> (a com.android.okhttp.ConnectionPool)\n" +
        "at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)\n" +
        "- locked <0x06842b17> (a com.android.okhttp.ConnectionPool)\n" +
        "at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)\n" +
        "at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:644)\n" +
        "at java.lang.Thread.run(Thread.java:1012)\n";
    return new ByteArrayInputStream(string.getBytes());
  }

  private void createTemporalFile() {
    try {
      Path tempPath = Files.createTempFile("rollbar-anr-timestamp", ".txt");
      Files.write(tempPath, ("" + ANR_TIMESTAMP).getBytes());
      file = tempPath.toFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void waitForDetectorToRun() throws InterruptedException {
    for(int i = 0; i<3 ; i++) {
      Thread.sleep(50);
    }
  }

  private void thenTheListenerIsCalled() {
    verify(anrListener).onAppNotResponding(any());
  }

  private void thenTheListenerIsNeverCalled() {
    verify(anrListener, never()).onAppNotResponding(any());
  }

  private void thenDebugLogMustSay(String logMessage) {
    verify(logger, times(1)).debug(logMessage);
  }

  private void thenWarningLogMustSay(String logMessage) {
    verify(logger, times(1)).warn(logMessage);
  }

  private void thenErrorLogMustSay(String logMessage) {
    verify(logger, times(1)).error(logMessage);
  }
}
