package com.rollbar.notifier.provider.notifier;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class VersionHelperTest {

  @Test
  public void shouldReturnNullIfNotAvailable() {
    VersionHelper helper = new VersionHelper();

    assertThat(helper.version(), is("unknown"));
  }
}