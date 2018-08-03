package com.rollbar.notifier.provider.notifier;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.data.Notifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class NotifierProviderTest {

  static final String VERSION = "1.3.0";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  VersionHelper versionHelper;

  NotifierProvider sut;

  @Before
  public void setUp() {
    when(versionHelper.version()).thenReturn(VERSION);

    sut = new NotifierProvider(versionHelper);
  }

  @Test
  public void shouldProvideTheNotifier() {
    Notifier expected = new Notifier.Builder()
        .name("rollbar-java")
        .version(VERSION)
        .build();

    Notifier result = sut.provide();

    assertThat(result, is(expected));
  }
}
