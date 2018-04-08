package com.rollbar.logback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Marker;

public class RollbarAppenderTest {

  private static String EXCEPTION_MESSAGE = "This is the exception message";

  private static final String FORMATTED_MESSAGE = "This is the logging message";

  private static final String LOGGER_NAME = "rollbar-logger";

  private static final String MARKER_NAME = "SQL_QUERY";

  private static final String THREAD_NAME = "my-thread";

  private static final Map<String, String> MDC = new HashMap<>();
  static {
    MDC.put("mdc_key_1", "mdc_value_1");
  }

  private static String NESTED_EXCEPTION_MESSAGE = "This is the nested exception message";

  private static final Exception NESTED_EXCEPTION =
      new NullPointerException(NESTED_EXCEPTION_MESSAGE);

  private static final Exception EXCEPTION =
      new IllegalArgumentException(EXCEPTION_MESSAGE, NESTED_EXCEPTION);

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private RollbarAppender sut;

  @Mock
  private Rollbar rollbar;

  @Mock
  private ILoggingEvent event;

  @Mock
  private Marker marker;

  private IThrowableProxy rootThrowableProxy = new ThrowableProxy(EXCEPTION);

  private ThrowableWrapper nestedThrowableWrapper = new RollbarThrowableWrapper(
      NESTED_EXCEPTION.getClass().getName(), NESTED_EXCEPTION.getMessage(),
      NESTED_EXCEPTION.getStackTrace(), null);

  private ThrowableWrapper rootThrowableWrapper = new RollbarThrowableWrapper(
      EXCEPTION.getClass().getName(), EXCEPTION.getMessage(), EXCEPTION.getStackTrace(),
      nestedThrowableWrapper);

  @Before
  public void setUp() {
    when(marker.getName()).thenReturn(MARKER_NAME);

    sut = new RollbarAppender(rollbar);
  }

  @Test
  public void shouldNotLogIfLoggerIsRollbar() {
    when(event.getLoggerName()).thenReturn("com.rollbar.any");

    sut.append(event);

    verify(rollbar, never()).log(any(ThrowableWrapper.class),
        ArgumentMatchers.<String, Object>anyMap(), anyString(), any(Level.class), anyBoolean());

  }

  @Test
  public void shouldLogEventWithAllInformationFromThrowableProxyWithThrowable() {
    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getMarker()).thenReturn(marker);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(ch.qos.logback.classic.Level.ERROR);
    when(event.getThrowableProxy()).thenReturn(rootThrowableProxy);
    when(event.getMDCPropertyMap()).thenReturn(MDC);
    when(event.getFormattedMessage()).thenReturn(FORMATTED_MESSAGE);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        new HashMap<String, Object>(MDC), MARKER_NAME, THREAD_NAME);

    verify(rollbar).log(rootThrowableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  @Test
  public void shouldLogEventWhenNoMarker() {
    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(ch.qos.logback.classic.Level.ERROR);
    when(event.getThrowableProxy()).thenReturn(rootThrowableProxy);
    when(event.getMDCPropertyMap()).thenReturn(MDC);
    when(event.getFormattedMessage()).thenReturn(FORMATTED_MESSAGE);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        new HashMap<String, Object>(MDC), null, THREAD_NAME);

    verify(rollbar).log(rootThrowableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  @Test
  public void shouldLogEventWhenNoMDC() {
    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getMarker()).thenReturn(marker);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(ch.qos.logback.classic.Level.ERROR);
    when(event.getThrowableProxy()).thenReturn(rootThrowableProxy);
    when(event.getFormattedMessage()).thenReturn(FORMATTED_MESSAGE);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        null, MARKER_NAME, THREAD_NAME);

    verify(rollbar).log(rootThrowableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  private static Map<String, Object> buildExpectedCustom(String loggerName, Map<String, Object> mdc,
      String markerName, String threadName) {
    Map<String, Object> rootCustom = new HashMap<>();
    Map<String, Object> custom = new HashMap<>();

    custom.put("loggerName", loggerName);
    custom.put("threadName", threadName);
    custom.put("marker", markerName);
    custom.put("mdc", mdc);

    rootCustom.put("rollbar-logback", custom);

    return rootCustom;
  }
}