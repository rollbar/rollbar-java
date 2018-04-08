package com.rollbar.log4j2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

  private static final List<String> NDC = Arrays.asList("ndc_value_1", "ndc_value_2");

  private static String NESTED_EXCEPTION_MESSAGE = "This is the nested exception message";

  private static final Exception NESTED_EXCEPTION =
      new NullPointerException(NESTED_EXCEPTION_MESSAGE);

  private static final Exception EXCEPTION =
      new IllegalArgumentException(EXCEPTION_MESSAGE, NESTED_EXCEPTION);

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private static final String APPENDER_NAME = "Rollbar";

  private RollbarAppender sut;

  @Mock
  private Rollbar rollbar;

  @Mock
  private LogEvent event;

  @Mock
  private Marker marker;

  @Mock
  private Message message;

  @Mock
  private ReadOnlyStringMap contextData;

  @Mock
  private ContextStack contextStack;

  @Before
  public void setUp() {
    when(marker.getName()).thenReturn(MARKER_NAME);
    when(contextData.toMap()).thenReturn(new HashMap<>(MDC));
    when(contextData.size()).thenReturn(MDC.size());
    when(contextStack.asList()).thenReturn(NDC);
    when(contextStack.size()).thenReturn(NDC.size());
    when(message.getFormattedMessage()).thenReturn(FORMATTED_MESSAGE);

    sut = new RollbarAppender(APPENDER_NAME, null, null, true, rollbar);
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
    ThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(EXCEPTION);

    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getMarker()).thenReturn(marker);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(org.apache.logging.log4j.Level.ERROR);
    when(event.getThrownProxy()).thenReturn(new ThrowableProxy(EXCEPTION));
    when(event.getContextData()).thenReturn(contextData);
    when(event.getContextStack()).thenReturn(contextStack);
    when(event.getMessage()).thenReturn(message);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        new HashMap<String, Object>(MDC), NDC, MARKER_NAME, THREAD_NAME);

    verify(rollbar).log(throwableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  @Test
  public void shouldLogEventWithAllInformationFromThrowableProxyWithoutThrowable() {
    ThrowableProxy throwableProxy = mock(ThrowableProxy.class);
    when(throwableProxy.getThrowable()).thenReturn(null);
    when(throwableProxy.getName()).thenReturn(EXCEPTION.getClass().getName());
    when(throwableProxy.getMessage()).thenReturn(EXCEPTION.getMessage());
    when(throwableProxy.getCauseProxy()).thenReturn(new ThrowableProxy(NESTED_EXCEPTION));
    when(throwableProxy.getStackTrace()).thenReturn(EXCEPTION.getStackTrace());

    ThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(EXCEPTION.getClass().getName(),
        EXCEPTION.getMessage(), EXCEPTION.getStackTrace(),
        new RollbarThrowableWrapper(NESTED_EXCEPTION));

    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getMarker()).thenReturn(marker);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(org.apache.logging.log4j.Level.ERROR);
    when(event.getThrownProxy()).thenReturn(throwableProxy);
    when(event.getContextData()).thenReturn(contextData);
    when(event.getContextStack()).thenReturn(contextStack);
    when(event.getMessage()).thenReturn(message);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        new HashMap<String, Object>(MDC), NDC, MARKER_NAME, THREAD_NAME);

    verify(rollbar).log(throwableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  @Test
  public void shouldLogEventWhenNoMarker() {
    RollbarThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(EXCEPTION);

    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(org.apache.logging.log4j.Level.ERROR);
    when(event.getThrownProxy()).thenReturn(new ThrowableProxy(EXCEPTION));
    when(event.getContextData()).thenReturn(contextData);
    when(event.getContextStack()).thenReturn(contextStack);
    when(event.getMessage()).thenReturn(message);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        new HashMap<String, Object>(MDC), NDC, null, THREAD_NAME);

    verify(rollbar).log(throwableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  @Test
  public void shouldLogEventWhenNoMDC() {
    RollbarThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(EXCEPTION);

    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getMarker()).thenReturn(marker);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(org.apache.logging.log4j.Level.ERROR);
    when(event.getThrownProxy()).thenReturn(new ThrowableProxy(EXCEPTION));
    when(event.getContextData()).thenReturn(null);
    when(event.getContextStack()).thenReturn(contextStack);
    when(event.getMessage()).thenReturn(message);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        null, NDC, MARKER_NAME, THREAD_NAME);

    verify(rollbar).log(throwableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  @Test
  public void shouldLogEventWhenNoNDC() {
    RollbarThrowableWrapper throwableWrapper = new RollbarThrowableWrapper(EXCEPTION);

    when(event.getLoggerName()).thenReturn(LOGGER_NAME);
    when(event.getMarker()).thenReturn(marker);
    when(event.getThreadName()).thenReturn(THREAD_NAME);
    when(event.getLevel()).thenReturn(org.apache.logging.log4j.Level.ERROR);
    when(event.getThrownProxy()).thenReturn(new ThrowableProxy(EXCEPTION));
    when(event.getContextData()).thenReturn(contextData);
    when(event.getContextStack()).thenReturn(null);
    when(event.getMessage()).thenReturn(message);

    sut.append(event);

    Map<String, Object> expectedCustom = buildExpectedCustom(LOGGER_NAME,
        new HashMap<String, Object>(MDC), null, MARKER_NAME, THREAD_NAME);

    verify(rollbar).log(throwableWrapper, expectedCustom, FORMATTED_MESSAGE, Level.ERROR, false);
  }

  private static Map<String, Object> buildExpectedCustom(String loggerName, Map<String, Object> mdc,
      List<String> ndc, String markerName, String threadName) {
    Map<String, Object> rootCustom = new HashMap<>();
    Map<String, Object> custom = new HashMap<>();

    custom.put("loggerName", loggerName);
    custom.put("threadName", threadName);
    custom.put("marker", markerName);
    custom.put("mdc", mdc);
    custom.put("ndc", ndc);

    rootCustom.put("rollbar-log4j2", custom);

    return rootCustom;
  }
}
