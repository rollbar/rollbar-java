package com.rollbar.log4j2;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;
import static org.apache.logging.log4j.core.util.Assert.isEmpty;

import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.notifier.config.ConfigProviderHelper;
import com.rollbar.notifier.provider.server.ServerProvider;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.util.Booleans;

@Plugin(name = "Rollbar", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE,
    printObject = true)
public class RollbarAppender extends AbstractAppender {

  private static final String PACKAGE_NAME = "com.rollbar";

  private static final String CUSTOM_NAMESPACE_KEY = "rollbar-log4j2";

  private static final String CUSTOM_LOGGER_NAME_KEY = "loggerName";

  private static final String CUSTOM_MDC_NAME_KEY = "mdc";

  private static final String CUSTOM_NDC_NAME_KEY = "ndc";

  private static final String CUSTOM_MAKER_NAME_KEY = "marker";

  private static final String CUSTOM_THREAD_NAME_KEY = "threadName";

  private Rollbar rollbar;

  protected RollbarAppender(String name, Filter filter, Layout<? extends Serializable> layout,
      boolean ignoreExceptions, Rollbar rollbar) {
    super(name, filter, layout, ignoreExceptions);
    this.rollbar = rollbar;
  }

  /**
   * Create appender plugin factory method.
   *
   * @param accessToken the Rollbar access token.
   * @param codeVersion the codeVersion.
   * @param endpoint the Rollbar endpoint to be used.
   * @param environment the environment.
   * @param language the language.
   * @param enabled to enable or disable Rollbar.
   * @param configProviderClassName The class name of the config provider implementation to get
   *     the configuration.
   * @param name the name.
   * @param layout the layout.
   * @param filter the filter.
   * @param ignore the ignore exceptions flag.
   * @return the rollbar appender.
   */
  @PluginFactory
  public static RollbarAppender createAppender(
      @PluginAttribute("accessToken") final String accessToken,
      @PluginAttribute("codeVersion") final String codeVersion,
      @PluginAttribute("endpoint") final String endpoint,
      @PluginAttribute("environment") final String environment,
      @PluginAttribute("language") final String language,
      @PluginAttribute(value = "enabled", defaultBoolean = true) final boolean enabled,
      @PluginAttribute("configProviderClassName") final String configProviderClassName,
      @PluginAttribute("name") @Required final String name,
      @PluginElement("Layout") Layout<? extends Serializable> layout,
      @PluginElement("Filter") Filter filter,
      @PluginAttribute("ignoreExceptions") final String ignore
  ) {
    // No @Required(a || b) in log4j, so we check this manually
    if (isEmpty(accessToken) && isEmpty(configProviderClassName)) {
      throw new ConfigurationException("Either accessToken or configProviderClassName must be "
              + "provided");
    }

    ConfigProvider configProvider = ConfigProviderHelper
        .getConfigProvider(configProviderClassName);
    Config config;

    ConfigBuilder configBuilder = withAccessToken(accessToken)
        .codeVersion(codeVersion)
        .environment(environment)
        .endpoint(endpoint)
        .server(new ServerProvider())
        .language(language)
        .enabled(enabled);

    if (configProvider != null) {
      config = configProvider.provide(configBuilder);
    } else {
      config = configBuilder.build();
    }

    Rollbar rollbar = new Rollbar(config);

    boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

    return new RollbarAppender(name, filter, layout, ignoreExceptions, rollbar);
  }

  @Override
  public void append(LogEvent event) {
    if (event.getLoggerName() != null && event.getLoggerName().startsWith(PACKAGE_NAME)) {
      LOGGER.warn("Recursive logging from [{}] for appender [{}].", event.getLoggerName(),
          getName());
      return;
    }

    ThrowableProxy throwableProxy = event.getThrownProxy();
    ThrowableWrapper rollbarThrowableWrapper = buildRollbarThrowableWrapper(throwableProxy);
    Map<String, Object> custom = this.buildCustom(event);
    String message = event.getMessage() != null ? event.getMessage().getFormattedMessage() : null;
    Level level = this.getLevel(event);

    rollbar.log(rollbarThrowableWrapper, custom, message, level, false);
  }

  @Override
  public boolean stop(final long timeout, final TimeUnit timeUnit) {
    super.stop(timeout, timeUnit);
    try {
      rollbar.close(true);
    } catch (Exception e) {
      LOGGER.error("Closing rollbar", e);
    }
    return true;
  }

  @Override
  public void stop() {
    this.stop(0, TimeUnit.MILLISECONDS);
  }

  private ThrowableWrapper buildRollbarThrowableWrapper(ThrowableProxy throwableProxy) {
    if (throwableProxy == null) {
      return null;
    }

    if (throwableProxy.getThrowable() != null) {
      return new RollbarThrowableWrapper(throwableProxy.getThrowable());
    }

    String className = throwableProxy.getName();
    String message = throwableProxy.getMessage();
    ThrowableWrapper causeThrowableWrapper =
        buildRollbarThrowableWrapper(throwableProxy.getCauseProxy());
    StackTraceElement[] stackTraceElements = buildStackTraceElements(
        throwableProxy.getStackTrace());

    return new RollbarThrowableWrapper(className, message, stackTraceElements,
        causeThrowableWrapper);
  }

  private StackTraceElement[] buildStackTraceElements(StackTraceElement[] stackTraceElements) {
    StackTraceElement[] elements = new StackTraceElement[stackTraceElements.length];

    for (int i = 0; i < stackTraceElements.length; i++) {
      elements[i] = stackTraceElements[i];
    }

    return elements;
  }

  private Level getLevel(LogEvent event) {
    org.apache.logging.log4j.Level level = event.getLevel();

    Level rollbarLevel = Level.lookupByName(level.name());
    if (rollbarLevel != null) {
      return rollbarLevel;
    }

    if (org.apache.logging.log4j.Level.FATAL.equals(level)) {
      return Level.CRITICAL;
    }

    return null;
  }

  private Map<String, Object> buildCustom(LogEvent event) {
    Map<String, Object> custom = new HashMap<>();

    custom.put(CUSTOM_LOGGER_NAME_KEY, event.getLoggerName());
    custom.put(CUSTOM_THREAD_NAME_KEY, event.getThreadName());

    custom.put(CUSTOM_MDC_NAME_KEY, this.buildMdc(event));
    custom.put(CUSTOM_NDC_NAME_KEY, this.getNdc(event));
    custom.put(CUSTOM_MAKER_NAME_KEY, this.getMarker(event));

    Map<String, Object> rootCustom = new HashMap<>();
    rootCustom.put(CUSTOM_NAMESPACE_KEY, custom);

    return rootCustom;
  }

  private Map<String, Object> buildMdc(LogEvent event) {
    if (event.getContextData() == null || event.getContextData().size() == 0) {
      return null;

    }

    Map<String, Object> mdc = new HashMap<>();

    for (Entry<String, String> mdcEntry : event.getContextData().toMap().entrySet()) {
      mdc.put(mdcEntry.getKey(), mdcEntry.getValue());
    }

    return mdc;
  }

  private List<String> getNdc(LogEvent event) {
    if (event.getContextStack() == null || event.getContextStack().size() == 0) {
      return null;
    }

    return event.getContextStack().asList();
  }

  private String getMarker(LogEvent event) {
    if (event.getMarker() == null) {
      return null;
    }

    return event.getMarker().getName();
  }
}
