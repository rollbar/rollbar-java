package com.rollbar.notifier.provider.notifier;

import com.rollbar.api.payload.data.Notifier;
import com.rollbar.notifier.provider.Provider;

/**
 * Default implementation to provide the {@link Notifier}.
 */
public class NotifierProvider implements Provider<Notifier> {

  private final Notifier notifier;

  public NotifierProvider() {
    this(new VersionHelper());
  }

  NotifierProvider(VersionHelper versionHelper) {
    String version = versionHelper.version();

    this.notifier = new Notifier.Builder()
        .name("rollbar-java")
        .version(version)
        .build();
  }

  @Override
  public Notifier provide() {
    return notifier;
  }
}
