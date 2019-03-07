package com.rollbar.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import android.util.Log;
import com.rollbar.android.provider.ClientProvider;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.android.provider.NotifierProvider;
import com.rollbar.android.provider.PersonProvider;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.sender.BufferedSender;
import com.rollbar.notifier.sender.queue.DiskQueue;


import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Rollbar {
  private static final String NOTIFIER_VERSION = "1.4.0";
  private static final String ITEM_DIR_NAME = "rollbar-items";
  private static final String ANDROID = "android";
  private static final String DEFAULT_ENVIRONMENT = "production";

  private static final int DEFAULT_ITEM_SCHEDULE_STARTUP_DELAY = 1;
  private static final int DEFAULT_ITEM_SCHEDULE_DELAY = 15;

  public static final String TAG = "Rollbar";
  private static final String ROLLBAR_NAMESPACE = "com.rollbar.android";
  private static final String MANIFEST_ACCESS_TOKEN = ROLLBAR_NAMESPACE + ".ACCESS_TOKEN";

  private com.rollbar.notifier.Rollbar rollbar;
  private static Rollbar notifier;

  private final int versionCode;
  private final String versionName;

  /**
   * Initialize the singleton instance of Rollbar.
   * Defaults to reading the access token from the manifest, handling uncaught exceptions, and setting
   * the environment to production.
   *
   * @param context Android context to use.
   * @return the managed instance of Rollbar.
   */
  public static Rollbar init(Context context) {
    return init(context, null, null);
  }

  /**
   * Initialize the singleton instance of Rollbar.
   *
   * @param context     Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @return the managed instance of Rollbar.
   */
  public static Rollbar init(Context context, String accessToken, String environment) {
    return init(context, accessToken, environment, true);
  }

  /**
   * Initialize the singleton instance of Rollbar.
   *
   * @param context     Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   * @return the managed instance of Rollbar.
   */
  public static Rollbar init(Context context, String accessToken, String environment, boolean registerExceptionHandler) {
    return init(context, accessToken, environment, registerExceptionHandler, false);
  }

  /**
   * Initialize the singleton instance of Rollbar.
   *
   * @param context     Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   * @param includeLogcat whether or not to include logcat output with items
   * @return the managed instance of Rollbar.
   */
  public static Rollbar init(Context context, String accessToken, String environment, boolean registerExceptionHandler, boolean includeLogcat) {
    return init(context, accessToken, environment, registerExceptionHandler, includeLogcat, null);
  }

  /**
   * Initialize the singleton instance of Rollbar.
   *
   * @param context     Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   * @param includeLogcat whether or not to include logcat output with items
   * @param provider a configuration provider that can be used to customize the configuration further.
   * @return the managed instance of Rollbar.
   */
  public static Rollbar init(Context context, String accessToken, String environment, boolean registerExceptionHandler, boolean includeLogcat, ConfigProvider provider) {
    if (isInit()) {
      Log.w(TAG, "Rollbar.init() called when it was already initialized.");
    } else {
      notifier = new Rollbar(context, accessToken, environment, registerExceptionHandler, includeLogcat, provider);
    }
    return notifier;
  }

  /**
   * Initialize the singleton instance of Rollbar.
   *
   * @param context  Android context to use.
   * @param provider a provider of the configuration.
   * @return the managed instance of Rollbar.
   */
  public static Rollbar init(Context context, ConfigProvider provider) {
    if (isInit()) {
      Log.w(TAG, "Rollbar.init() called when it was already initialized.");
    } else {
      notifier = new Rollbar(context, null, null, true, false, provider);
    }
    return notifier;
  }

  /**
   * Has the singleton instance of Rollbar been initialized already or not.
   *
   * @return true if init has already been called.
   */
  public static boolean isInit() {
    return notifier != null;
  }

  /**
   * Get access to the managed instance of Rollbar.
   *
   * @return the managed singleton.
   */
  public static Rollbar instance() {
    if (isInit()) {
      return notifier;
    } else {
      Log.w(TAG, "Attempt to access Rollbar.instance() before initialization.");
      return null;
    }
  }

  /**
   * Construct a new Rollbar instance.
   *
   * @param context Android context to use.
   */
  public Rollbar(Context context) {
    this(context, null, null, true);
  }

  /**
   * Construct a new Rollbar instance.
   *
   * @param context Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   */
  public Rollbar(Context context, String accessToken, String environment, boolean registerExceptionHandler) {
    this(context, accessToken, environment, registerExceptionHandler, false, null);
  }

  /**
   * Construct a new Rollbar instance.
   *
   * @param context Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   * @param includeLogcat whether or not to include logcat output with items
   */
  public Rollbar(Context context, String accessToken, String environment, boolean registerExceptionHandler, boolean includeLogcat) {
    this(context, accessToken, environment, registerExceptionHandler, includeLogcat, null);
  }

  /**
   * Construct a new Rollbar instance.
   *
   * @param context Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   * @param includeLogcat whether or not to include logcat output with items
   * @param configProvider a configuration provider that can be used to customize the configuration further.
   */
  public Rollbar(Context context, String accessToken, String environment, boolean registerExceptionHandler, boolean includeLogcat, ConfigProvider configProvider) {
    this(context, accessToken, environment, registerExceptionHandler, includeLogcat, configProvider, "full");
  }

  /**
   * Construct a new Rollbar instance.
   *
   * @param context Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   * @param includeLogcat whether or not to include logcat output with items
   * @param configProvider a configuration provider that can be used to customize the configuration further.
   * @param captureIp one of: full, anonymize, none. This determines how the remote ip is captured.
   */
  public Rollbar(Context context, String accessToken, String environment, boolean registerExceptionHandler, boolean includeLogcat, ConfigProvider configProvider, String captureIp) {
    this(context, accessToken, environment, registerExceptionHandler, includeLogcat, configProvider, captureIp, -1);
  }

  /**
   * Construct a new Rollbar instance.
   *
   * @param context Android context to use.
   * @param accessToken a Rollbar access token with at least post_client_item scope
   * @param environment the environment to set for items
   * @param registerExceptionHandler whether or not to handle uncaught exceptions.
   * @param includeLogcat whether or not to include logcat output with items
   * @param configProvider a configuration provider that can be used to customize the configuration further.
   * @param captureIp one of: full, anonymize, none. This determines how the remote ip is captured.
   * @param maxLogcatSize the maximum number of logcat lines to capture with items (ignored unless >= 0)
   */
  public Rollbar(Context context, String accessToken, String environment, boolean registerExceptionHandler, boolean includeLogcat, ConfigProvider configProvider, String captureIp, int maxLogcatSize) {
    if (accessToken == null) {
      try {
        accessToken = loadAccessTokenFromManifest(context);
      } catch (NameNotFoundException e) {
        Log.e(TAG, "Error getting access token from manifest.");
      }
    }

    PackageInfo info = null;
    try {
      String packageName = context.getPackageName();
      info = context.getPackageManager().getPackageInfo(packageName, 0);
    } catch (NameNotFoundException e) {
      Log.e(TAG, "Error getting package info.");
    }
    versionCode = info != null ? info.versionCode : 0;
    versionName = info != null ? info.versionName : "unknown";

    ClientProvider clientProvider = new ClientProvider.Builder()
        .versionCode(versionCode)
        .versionName(versionName)
        .includeLogcat(includeLogcat)
        .captureIp(captureIp)
        .maxLogcatSize(maxLogcatSize)
        .build();

    File folder = new File(context.getCacheDir(), ITEM_DIR_NAME);

    ConfigBuilder defaultConfig = ConfigBuilder.withAccessToken(accessToken)
        .client(clientProvider)
        .platform(ANDROID)
        .framework(ANDROID)
        .notifier(new NotifierProvider(NOTIFIER_VERSION))
        .environment(environment == null ? DEFAULT_ENVIRONMENT : environment)
        .handleUncaughtErrors(registerExceptionHandler);

    Config config;
    if (configProvider != null) {
      config = configProvider.provide(defaultConfig);
    } else {
      config = defaultConfig.build();
    }

    if (config.sender() == null) {
      DiskQueue queue = new DiskQueue.Builder()
              .queueFolder(folder)
              .build();

      BufferedSender sender = new BufferedSender.Builder()
              .queue(queue)
              .initialFlushDelay(TimeUnit.SECONDS.toMillis(DEFAULT_ITEM_SCHEDULE_STARTUP_DELAY))
              .flushFreq(TimeUnit.SECONDS.toMillis(DEFAULT_ITEM_SCHEDULE_DELAY))
              .build();

      config = ConfigBuilder.withConfig(config)
              .sender(sender)
              .build();
    }

    this.rollbar = new com.rollbar.notifier.Rollbar(config);
  }

  /**
   * Set the person data to include with future items.
   *
   * @param id an identifier for this user.
   * @param username the username of the user.
   * @param email the email of this user.
   */
  public void setPersonData(final String id, final String username, final String email) {
    this.rollbar.configure(new ConfigProvider() {
      @Override
      public Config provide(ConfigBuilder builder) {
        return builder
            .person(new PersonProvider(id, username, email))
            .build();
      }
    });
  }

  /**
   * Remove any person data that might be set.
   */
  public void clearPersonData() {
    this.rollbar.configure(new ConfigProvider() {
      @Override
      public Config provide(ConfigBuilder builder) {
        return builder
            .person(null)
            .build();
      }
    });
  }

  /**
   * Toggle whether to include logcat output in items.
   *
   * @param includeLogcat whether or not to include logcat output.
   */
  public void setIncludeLogcat(final boolean includeLogcat) {
    final int versionCode = this.versionCode;
    final String versionName = this.versionName;
    this.rollbar.configure(new ConfigProvider() {
      @Override
      public Config provide(ConfigBuilder builder) {
        ClientProvider clientProvider = new ClientProvider.Builder()
            .versionCode(versionCode)
            .versionName(versionName)
            .includeLogcat(includeLogcat)
            .build();
        return builder
            .client(clientProvider)
            .build();
      }
    });
  }

  /**
   * Update the configuration of this instance.
   *
   * @param configProvider an object conforming to {@link ConfigProvider} which will
   *                       be called with a builder that has all of the current
   */
  public void configure(ConfigProvider configProvider) {
    this.rollbar.configure(configProvider);
  }

  /**
   * Update the configuration of this instance directly.
   *
   * @param config the new configuration.
   */
  public void configure(Config config) {
    this.rollbar.configure(config);
  }

  /**
   * Record a critical error.
   *
   * @param error the error.
   */
  public void critical(Throwable error) {
    critical(error, null, null);
  }

  /**
   * Record a critical error with human readable description.
   *
   * @param error       the error.
   * @param description human readable description of error.
   */
  public void critical(Throwable error, String description) {
    critical(error, null, description);
  }

  /**
   * Record a critical error with extra information attached.
   *
   * @param error  the error.
   * @param custom the extra information.
   */
  public void critical(Throwable error, Map<String, Object> custom) {
    critical(error, custom, null);
  }

  /**
   * Record a critical message.
   *
   * @param message the message.
   */
  public void critical(String message) {
    critical(null, null, message);
  }

  /**
   * Record a critical message with extra information attached.
   *
   * @param message the message.
   * @param custom  the extra information.
   */
  public void critical(String message, Map<String, Object> custom) {
    critical(null, custom, message);
  }

  /**
   * Record a critical error with custom parameters and human readable description.
   *
   * @param error       the error.
   * @param custom      the custom data.
   * @param description the human readable description of error.
   */
  public void critical(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.CRITICAL);
  }

  /**
   * Record an error.
   *
   * @param error the error.
   */
  public void error(Throwable error) {
    error(error, null, null);
  }

  /**
   * Record an error with human readable description.
   *
   * @param error       the error.
   * @param description human readable description of error.
   */
  public void error(Throwable error, String description) {
    error(error, null, description);
  }

  /**
   * Record an error with extra information attached.
   *
   * @param error  the error.
   * @param custom the extra information.
   */
  public void error(Throwable error, Map<String, Object> custom) {
    error(error, custom, null);
  }

  /**
   * Record an error message.
   *
   * @param message the message.
   */
  public void error(String message) {
    error(null, null, message);
  }

  /**
   * Record a error message with extra information attached.
   *
   * @param message the message.
   * @param custom  the extra information.
   */
  public void error(String message, Map<String, Object> custom) {
    error(null, custom, message);
  }

  /**
   * Record an error with custom parameters and human readable description.
   *
   * @param error       the error.
   * @param custom      the custom data.
   * @param description the human readable description of error.
   */
  public void error(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.ERROR);
  }

  /**
   * Record an error as a warning.
   *
   * @param error the error.
   */
  public void warning(Throwable error) {
    warning(error, null, null);
  }

  /**
   * Record a warning with human readable description.
   *
   * @param error       the error.
   * @param description human readable description of error.
   */
  public void warning(Throwable error, String description) {
    warning(error, null, description);
  }

  /**
   * Record a warning error with extra information attached.
   *
   * @param error  the error.
   * @param custom the extra information.
   */
  public void warning(Throwable error, Map<String, Object> custom) {
    warning(error, custom, null);
  }

  /**
   * Record a warning message.
   *
   * @param message the message.
   */
  public void warning(String message) {
    warning(null, null, message);
  }

  /**
   * Record a warning message with extra information attached.
   *
   * @param message the message.
   * @param custom  the extra information.
   */
  public void warning(String message, Map<String, Object> custom) {
    warning(null, custom, message);
  }

  /**
   * Record a warning error with custom parameters and human readable description.
   *
   * @param error       the error.
   * @param custom      the custom data.
   * @param description the human readable description of error.
   */
  public void warning(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.WARNING);
  }

  /**
   * Record an error as an info.
   *
   * @param error the error.
   */
  public void info(Throwable error) {
    info(error, null, null);
  }

  /**
   * Record an info error with human readable description.
   *
   * @param error       the error.
   * @param description human readable description of error.
   */
  public void info(Throwable error, String description) {
    info(error, null, description);
  }

  /**
   * Record an info error with extra information attached.
   *
   * @param error  the error.
   * @param custom the extra information.
   */
  public void info(Throwable error, Map<String, Object> custom) {
    info(error, custom, null);
  }

  /**
   * Record an informational message.
   *
   * @param message the message.
   */
  public void info(String message) {
    info(null, null, message);
  }

  /**
   * Record an informational message with extra information attached.
   *
   * @param message the message.
   * @param custom  the extra information.
   */
  public void info(String message, Map<String, Object> custom) {
    info(null, custom, message);
  }

  /**
   * Record an info error with custom parameters and human readable description.
   *
   * @param error       the error.
   * @param custom      the custom data.
   * @param description the human readable description of error.
   */
  public void info(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.INFO);
  }

  /**
   * Record an error as debugging information.
   *
   * @param error the error.
   */
  public void debug(Throwable error) {
    debug(error, null, null);
  }

  /**
   * Record a debug error with human readable description.
   *
   * @param error       the error.
   * @param description human readable description of error.
   */
  public void debug(Throwable error, String description) {
    debug(error, null, description);
  }

  /**
   * Record a debug error with extra information attached.
   *
   * @param error  the error.
   * @param custom the extra information.
   */
  public void debug(Throwable error, Map<String, Object> custom) {
    debug(error, custom, null);
  }

  /**
   * Record a debugging message.
   *
   * @param message the message.
   */
  public void debug(String message) {
    debug(null, null, message);
  }

  /**
   * Record a debugging message with extra information attached.
   *
   * @param message the message.
   * @param custom  the extra information.
   */
  public void debug(String message, Map<String, Object> custom) {
    debug(null, custom, message);
  }

  /**
   * Record a debug error with custom parameters and human readable description.
   *
   * @param error       the error.
   * @param custom      the custom data.
   * @param description the human readable description of error.
   */
  public void debug(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, Level.DEBUG);
  }

  /**
   * Log an error at the level returned by {@link com.rollbar.notifier.Rollbar#level}.
   *
   * @param error the error.
   */
  public void log(Throwable error) {
    log(error, null, null, null);
  }

  /**
   * Record an error with human readable description at the default level returned by {@link
   * com.rollbar.notifier.Rollbar#level}.
   *
   * @param error       the error.
   * @param description human readable description of error.
   */
  public void log(Throwable error, String description) {
    log(error, null, description, null);
  }

  /**
   * Record an error with extra information attached at the default level returned by {@link
   * com.rollbar.notifier.Rollbar#level}.
   *
   * @param error  the error.
   * @param custom the extra information.
   */
  public void log(Throwable error, Map<String, Object> custom) {
    log(error, custom, null, null);
  }

  /**
   * Record an error with extra information attached at the level specified.
   *
   * @param error  the error.
   * @param custom the extra information.
   * @param level  the level.
   */
  public void log(Throwable error, Map<String, Object> custom, Level level) {
    log(error, custom, null, level);
  }

  /**
   * Log an error at level specified.
   *
   * @param error the error.
   * @param level the level of the error.
   */
  public void log(Throwable error, Level level) {
    log(error, null, null, level);
  }

  /**
   * Record a debug error with human readable description at the specified level.
   *
   * @param error       the error.
   * @param description human readable description of error.
   * @param level       the level.
   */
  public void log(Throwable error, String description, Level level) {
    log(error, null, description, level);
  }

  /**
   * Record an error with custom parameters and human readable description at the default level
   * returned by {@link com.rollbar.notifier.Rollbar#level}.
   *
   * @param error       the error.
   * @param custom      the custom data.
   * @param description the human readable description of error.
   */
  public void log(Throwable error, Map<String, Object> custom, String description) {
    log(error, custom, description, null);
  }

  /**
   * Record a debugging message at the level returned by {@link com.rollbar.notifier.Rollbar#level} (WARNING unless level
   * is overridden).
   *
   * @param message the message.
   */
  public void log(String message) {
    log(null, null, message, null);
  }

  /**
   * Record a message with extra information attached at the default level returned by {@link
   * com.rollbar.notifier.Rollbar#level}, (WARNING unless level overridden).
   *
   * @param message the message.
   * @param custom  the extra information.
   */
  public void log(String message, Map<String, Object> custom) {
    log(null, custom, message, null);
  }

  /**
   * Record a message at the level specified.
   *
   * @param message the message.
   * @param level   the level.
   */
  public void log(String message, Level level) {
    log(null, null, message, level);
  }

  /**
   * Record a message with extra information attached at the specified level.
   *
   * @param message the message.
   * @param custom  the extra information.
   * @param level   the level.
   */
  public void log(String message, Map<String, Object> custom, Level level) {
    log(null, custom, message, level);
  }

  /**
   * Record an error or message with extra data at the level specified. At least ene of `error` or
   * `description` must be non-null. If error is null, `description` will be sent as a message. If
   * error is non-null, description will be sent as the description of the error. Custom data will
   * be attached to message if the error is null. Custom data will extend whatever {@link
   * Config#custom} returns.
   *
   * @param error       the error (if any).
   * @param custom      the custom data (if any).
   * @param description the description of the error, or the message to send.
   * @param level       the level to send it at.
   */
  public void log(final Throwable error, final Map<String, Object> custom, final String description, final Level level) {
    rollbar.log(error, custom, description, level);
  }

  /**
   * report an exception to Rollbar
   * @param throwable the exception that occurred.
   */
  @Deprecated
  public static void reportException(Throwable throwable) {
    reportException(throwable, null, null, null);
  }

  /**
   * report an exception to Rollbar, specifying the level.
   *
   * @param throwable the exception that occurred.
   * @param level the severity level.
   */
  @Deprecated
  public static void reportException(final Throwable throwable, final String level) {
    reportException(throwable, level, null, null);
  }

  /**
   * report an exception to Rollbar, specifying the level, and adding a custom description.
   * @param throwable the exception that occurred.
   * @param level the severity level.
   * @param description the extra description.
   */
  @Deprecated
  public static void reportException(final Throwable throwable, final String level, final String description) {
    reportException(throwable, level, description, null);
  }

  /**
   * report an exception to Rollbar, specifying the level, adding a custom description,
   * and including extra data.
   *
   * @param throwable the exception that occurred.
   * @param level the severity level.
   * @param description the extra description.
   * @param params the extra custom data.
   */
  @Deprecated
  public static void reportException(final Throwable throwable, final String level, final String description, final Map<String, String> params) {
    ensureInit(new Runnable() {
      @Override
      public void run() {
        notifier.log(throwable, params != null ? Collections.<String, Object>unmodifiableMap(params) : null, description, Level.lookupByName(level));
      }
    });
  }

  /**
   * Report a message to Rollbar.
   *
   * @param message the message to send.
   */
  @Deprecated
  public static void reportMessage(String message) {
    reportMessage(message, null);
  }

  /**
   * Report a message to Rollbar, specifying the level.
   *
   * @param message the message to send.
   * @param level the severity level.
   */
  @Deprecated
  public static void reportMessage(final String message, final String level) {
    reportMessage(message, level, null);
  }

  /**
   * Report a message to Rollbar, specifying the level, and including extra data.
   *
   * @param message the message to send.
   * @param level the severity level.
   * @param params the extra data.
   */
  @Deprecated
  public static void reportMessage(final String message, final String level, final Map<String, String> params) {
    ensureInit(new Runnable() {
      @Override
      public void run() {
        notifier.log(message, params != null ? Collections.<String, Object>unmodifiableMap(params) : null, Level.lookupByName(level));
      }
    });
  }

  private String loadAccessTokenFromManifest(Context context) throws NameNotFoundException {
    Context appContext = context.getApplicationContext();
    ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
    Bundle data = ai.metaData;
    return data.getString(MANIFEST_ACCESS_TOKEN);
  }

  private static void ensureInit(Runnable runnable) {
    if (isInit()) {
      try {
        runnable.run();
      } catch (Exception e) {
        Log.e(TAG, "Exception when interacting with Rollbar", e);
      }
    } else {
      Log.e(TAG, "Rollbar not initialized with an access token!");
    }
  }

}
