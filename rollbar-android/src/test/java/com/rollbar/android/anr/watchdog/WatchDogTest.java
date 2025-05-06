package com.rollbar.android.anr.watchdog;

import static android.app.ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;

import com.rollbar.android.anr.AnrException;
import com.rollbar.android.anr.AnrListener;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.provider.timestamp.TimestampProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class WatchDogTest {
  private static final long ANR_TIMEOUT_MILLIS = 5000;

  private long currentTimeMs = 0L;
  private AnrException anrException;
  private final AnrListener anrListener = new AnrListenerFake();
  private final Thread.State blockedState = Thread.State.BLOCKED;
  private final StackTraceElement stacktrace = new StackTraceElement("declaringClass",
      "methodName", "fileName", 7);

  @Mock
  private Thread thread;

  @Mock
  private ActivityManager activityManager;

  @Mock
  private Context context;

  @Mock
  private LooperHandler looperHandler;

  private final Provider<Long> timeProvider = new TimestampProviderFake();

  private WatchDog watchDog;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    currentTimeMs = 0;
    watchDog = new WatchDog(
        context,
        anrListener,
        looperHandler,
        new WatchdogConfiguration.Builder().build(),
        timeProvider
    );
  }

  @After
  public void tearDown() {
    watchDog.interrupt();
  }

  @Test
  public void shouldNotDetectAnrWhenTimeOutIsNotSurpassed() throws InterruptedException {
    whenWatchdogStart();
    whenAnrTimeOutIsNotSurpassed();

    thenAnrIsNotDetected();
  }

  @Test
  public void shouldDetectAnrWhenMainThreadIsBlockedAndActivityManagerNotAvailable() throws InterruptedException {
    givenABlockedThread();

    whenWatchdogStart();
    whenAnrTimeOutIsSurpassed();

    thenAnrExceptionIsTheExpected();
  }

  @Test
  public void shouldDetectAnrWhenMainThreadIsBlockedAndActivityManagerHasAnr() throws InterruptedException {
    givenAnActivityManagerWithAnr();
    givenABlockedThread();

    whenWatchdogStart();
    whenAnrTimeOutIsSurpassed();

    thenAnrExceptionIsTheExpected();
  }

  private void thenAnrIsNotDetected() {
    assertNull(anrException);
  }

  private void thenAnrExceptionIsTheExpected() {
    assertNotNull(anrException);
    assertEquals(anrException.getMessage(), "Application Not Responding for at least 5000 ms.");
    assertEquals(stacktrace.getClassName(), anrException.getStackTrace()[0].getClassName());
  }

  private void whenWatchdogStart() {
    watchDog.start();
  }

  private void whenAnrTimeOutIsNotSurpassed() throws InterruptedException {
    int iterations = 0;
    int maxIterations = 10; //just to prevent infinite execution

    while (iterations < maxIterations) {
      iterations++;
      currentTimeMs += 1;
      defaultSleep();
    }
  }

  private void whenAnrTimeOutIsSurpassed() throws InterruptedException {
    int iterations = 0;
    int maxIterations = 30; //just to prevent infinite execution

    while (anrException == null && iterations < maxIterations) {
      iterations++;
      currentTimeMs += ANR_TIMEOUT_MILLIS + 1;
      defaultSleep();
    }
  }

  private void givenAnActivityManagerWithAnr() {
    ActivityManager.ProcessErrorStateInfo stateInfo = new ActivityManager.ProcessErrorStateInfo();
    stateInfo.condition = NOT_RESPONDING;
    List<ActivityManager.ProcessErrorStateInfo> anrs = new ArrayList<>();
    anrs.add(stateInfo);

    when(context.getSystemService(eq(Context.ACTIVITY_SERVICE))).thenReturn(activityManager);
    when(activityManager.getProcessesInErrorState()).thenReturn(anrs);
  }

  private void givenABlockedThread() {
    StackTraceElement[] stackTraceElements = {stacktrace};

    when(thread.getState()).thenReturn(blockedState);
    when(thread.getStackTrace()).thenReturn(stackTraceElements);
    when(looperHandler.getThread()).thenReturn(thread);
  }

  private void defaultSleep() throws InterruptedException {
    Thread.sleep(20);
  }

  private class TimestampProviderFake extends TimestampProvider {
    @Override
    public Long provide() {
      return currentTimeMs;
    }
  }

  private class AnrListenerFake implements AnrListener {
    @Override
    public void onAppNotResponding(AnrException error) {
      anrException = error;
    }
  }
}
