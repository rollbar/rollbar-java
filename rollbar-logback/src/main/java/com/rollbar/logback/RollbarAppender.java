package com.rollbar.logback;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RollbarAppender extends AppenderBase<ILoggingEvent> {

  private static final String PACKAGE_NAME = "com.rollbar";

  private static final String CUSTOM_NAMESPACE_KEY = "rollbar-logback";

  private static final String CUSTOM_LOGGER_NAME_KEY = "loggerName";

  private static final String CUSTOM_MDC_NAME_KEY = "mdc";

  private static final String CUSTOM_MAKER_NAME_KEY = "marker";

  private static final String CUSTOM_THREAD_NAME_KEY = "threadName";
  
  private Rollbar rollbar;

  private String accessToken;

  /**
   * Constructor for testing purposes.
   *
   * @param rollbar the rollbar notifier.
   */
  protected RollbarAppender(Rollbar rollbar) {
    this.rollbar = rollbar;
  }

  @Override
  public void start() {
    this.rollbar = new Rollbar(withAccessToken(this.accessToken).build());

    super.start();
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (event.getLoggerName() != null && event.getLoggerName().startsWith(PACKAGE_NAME)) {
      addWarn("Recursive logging");
      return;
    }

    IThrowableProxy throwableProxy = event.getThrowableProxy();
    ThrowableWrapper rollbarThrowableWrapper = buildRollbarThrowableWrapper(throwableProxy);
    Map<String, Object> custom = this.buildCustom(event);

    rollbar.log(rollbarThrowableWrapper, custom, event.getFormattedMessage(),
        Level.lookupByName(event.getLevel().levelStr), false);

  }

  /**
   * The configuration Rollbar access token.
   *
   * @param accessToken The Rollbar access token.
   */
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  private ThrowableWrapper buildRollbarThrowableWrapper(IThrowableProxy throwableProxy) {
    if (throwableProxy == null) {
      return null;
    }

    String className = throwableProxy.getClassName();
    String message = throwableProxy.getMessage();
    ThrowableWrapper causeThrowableWrapper =
        buildRollbarThrowableWrapper(throwableProxy.getCause());
    StackTraceElement[] stackTraceElements = buildStackTraceElements(
        throwableProxy.getStackTraceElementProxyArray());

    return new RollbarThrowableWrapper(className, message, stackTraceElements,
        causeThrowableWrapper);
  }

  private StackTraceElement[] buildStackTraceElements(StackTraceElementProxy[] stackTraceElements) {
    StackTraceElement[] elements = new StackTraceElement[stackTraceElements.length];

    for (int i = 0; i < stackTraceElements.length; i++) {
      elements[i] = stackTraceElements[i].getStackTraceElement();
    }

    return elements;
  }

  private Map<String, Object> buildCustom(ILoggingEvent event) {
    Map<String, Object> custom = new HashMap<>();

    custom.put(CUSTOM_LOGGER_NAME_KEY, event.getLoggerName());
    custom.put(CUSTOM_THREAD_NAME_KEY, event.getThreadName());

    custom.put(CUSTOM_MDC_NAME_KEY, this.buildMdc(event));
    custom.put(CUSTOM_MAKER_NAME_KEY, this.getMarker(event));

    Map<String, Object> rootCustom = new HashMap<>();
    rootCustom.put(CUSTOM_NAMESPACE_KEY, custom);

    return rootCustom;
  }

  private Map<String, Object> buildMdc(ILoggingEvent event) {
    if (event.getMDCPropertyMap() == null || event.getMDCPropertyMap().size() == 0) {
      return null;
    }
    
    Map<String, Object> custom = new HashMap<>();

    for (Entry<String, String> mdcEntry : event.getMDCPropertyMap().entrySet()) {
      custom.put(mdcEntry.getKey(), mdcEntry.getValue());
    }

    return custom;
  }

  private String getMarker(ILoggingEvent event) {
    if (event.getMarker() == null) {
      return null;
    }

    return event.getMarker().getName();
  }
}
