package com.rollbar.android.anr;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

public class AnrDetectorFactoryTest {
  @Mock
  private Context context;

  @Mock
  private AnrListener anrListener;

  @Mock
  private Logger logger;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void createShouldReturnNullWhenNoAnrConfigurationIsProvided() {
    AnrDetector anrDetector = AnrDetectorFactory.create(context, logger, null, anrListener);

    assertNull(anrDetector);
    thenWarningLogMustSay("No ANR configuration");
  }

  @Test
  public void createShouldReturnNullWhenNoContextIsProvided() {
    AnrDetector anrDetector = AnrDetectorFactory.create(null, logger, new AnrConfiguration.Builder().build(), anrListener);

    assertNull(anrDetector);
    thenWarningLogMustSay("No context");
  }

  private void thenWarningLogMustSay(String logMessage) {
    verify(logger, times(1)).warn(logMessage);
  }
}
