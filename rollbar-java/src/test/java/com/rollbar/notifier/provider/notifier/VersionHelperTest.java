package com.rollbar.notifier.provider.notifier;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

public class VersionHelperTest {
  @Test
  public void shouldReturnVersion() {
    VersionHelper helper = new VersionHelper();
    // It will fail when we upgrade to 2.x, but it's stable enough. Better than nothing when running
    // from an IDE, without the version property that we set in Gradle.
    assertThat(helper.version(), startsWith("1."));
  }

  @Test
  public void versionReturnedShouldMatchManifestVersion() {
    // We set this in Gradle since there's no jar manifest available when running tests.
    String expectedVersion = System.getProperty("ROLLBAR_IMPLEMENTATION_VERSION");
    assumeThat(expectedVersion, not(isEmptyOrNullString()));

    VersionHelper helper = new VersionHelper();
    assertThat(helper.version(), equalTo(expectedVersion));
  }
}
