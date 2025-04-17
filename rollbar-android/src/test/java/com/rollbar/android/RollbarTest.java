package com.rollbar.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import com.rollbar.android.provider.NotifierProvider;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.notifier.filter.Filter;
import com.rollbar.notifier.sender.BufferedSender;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.sender.queue.DiskQueue;
import com.rollbar.notifier.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;


public class RollbarTest {
  static final String ACCESS_TOKEN = "access_token";

  static final String ENVIRONMENT = "environment";

  static final String CODE_VERSION = "code_version";

  static final String PLATFORM = "platform";

  static final String LANGUAGE = "language";

  static final String FRAMEWORK = "framework";

  static final String PACKAGE_NAME = "package_name";

  // Android mocks
  @Mock
  Context mockApplicationContext;

  @Mock
  PackageManager mockPackageManager;

  @Mock
  PackageInfo mockPackageInfo;

  // Rollbar-java mocks
  @Mock
  Filter filter;

  @Mock
  Sender sender;

  @Mock
  Transformer transformer;

  @Mock
  ApplicationInfo applicationInfo;

  @Mock
  Bundle bundle;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(mockApplicationContext.getPackageName()).thenReturn(PACKAGE_NAME);
    when(mockApplicationContext.getPackageManager()).thenReturn(mockPackageManager);

    when(mockPackageManager.getPackageInfo(anyString(), anyInt()))
            .thenReturn(mockPackageInfo);
    when(mockPackageManager.getApplicationInfo(eq(PACKAGE_NAME), eq(PackageManager.GET_META_DATA)))
            .thenReturn(applicationInfo);
    when(bundle.getString(eq("com.rollbar.android._notifier.version"))).thenReturn("1.7.0");

    mockPackageInfo.versionCode = 23;
    mockPackageInfo.versionName = "version name";
    applicationInfo.metaData = this.bundle;
  }

  @Test
  public void shouldUseDefaultBufferedSenderAndDiskQueue() {
    Config config;

    Rollbar sut = new Rollbar(mockApplicationContext, ACCESS_TOKEN, ENVIRONMENT, true);

    config = sut.config();

    assertThat(config.sender(), instanceOf(BufferedSender.class));
    assertThat(((BufferedSender)config.sender()).queue(), instanceOf(DiskQueue.class));
    assertThat(((BufferedSender)config.sender()).sender(), instanceOf(SyncSender.class));
  }

  @Test
  public void shouldUseCustomSender() {
    Config config;
    final SyncSender syncSender = new SyncSender.Builder().build();

    Rollbar sut = new Rollbar(mockApplicationContext, ACCESS_TOKEN, ENVIRONMENT, true, false, new ConfigProvider() {
      @Override
      public Config provide(ConfigBuilder builder) {
        return builder
          .sender(syncSender)
          .build();
      }
    });

    config = sut.config();

    assertThat(config.sender(), instanceOf(SyncSender.class));
  }

  @Test
  public void shouldUseDefaultNotifier() {
    Config config;

    Rollbar sut = new Rollbar(mockApplicationContext, ACCESS_TOKEN, ENVIRONMENT, true);

    config = sut.config();

    assertEquals(config.notifier().provide().getName(), "rollbar-android");
  }

  @Test
  public void shouldUseCustomNotifier() {
    Config config;
    final String notifierVersion = "1.2.3";
    final String notifierName = "custom-notifier";

    Rollbar sut = new Rollbar(mockApplicationContext, ACCESS_TOKEN, ENVIRONMENT, true, false, new ConfigProvider() {
      @Override
      public Config provide(ConfigBuilder builder) {
        return builder
          .notifier(new NotifierProvider(notifierVersion, notifierName))
          .build();
      }
    });

    config = sut.config();

    assertEquals(config.notifier().provide().getVersion(), notifierVersion);
    assertEquals(config.notifier().provide().getName(), notifierName);
  }

  @Test
  public void shouldUseAndroidPackageInfo() {
    Config config;

    Rollbar sut = new Rollbar(mockApplicationContext, ACCESS_TOKEN, ENVIRONMENT, true);

    config = sut.config();

    assertEquals(config.client().provide().getTopLevelData().get("code_version"), 23);
    assertEquals(config.client().provide().getTopLevelData().get("name_version"), "version name");
  }

  @Test
  public void shouldSendPayload() {
    Throwable error = new RuntimeException("Something went wrong.");
    Config config;

    Rollbar sut = new Rollbar(mockApplicationContext, ACCESS_TOKEN, ENVIRONMENT, true, false, new ConfigProvider() {
      @Override
      public Config provide(ConfigBuilder builder) {
        return builder
          .sender(sender)
          .build();
      }
    });

    config = sut.config();

    sut.log(error);

    verify(config.sender()).send(any(Payload.class));
  }

  @Test
  public void shouldPostFilterWithoutSendingPayload() {
    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    String description = "description";
    Map<String, Object> custom = new HashMap<>();
    Config config;

    when(transformer.transform(any(Data.class))).thenReturn(mock(Data.class));
    when(filter.preProcess(level, error, custom, description)).thenReturn(false);
    when(filter.postProcess(any(Data.class))).thenReturn(true);

    Rollbar sut = new Rollbar(mockApplicationContext, ACCESS_TOKEN, ENVIRONMENT, true, false, new ConfigProvider() {
      @Override
      public Config provide(ConfigBuilder builder) {
        return builder
          .transformer(transformer)
          .filter(filter)
          .sender(sender)
          .build();
      }
    });

    sut.log(error, custom, description, level);

    config = sut.config();

    verify(config.sender(), never()).send(any(Payload.class));
  }
}
