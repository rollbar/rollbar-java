package com.rollbar.notifier.provider.notifier;

class VersionHelper {

  public String version() {
    String version = VersionHelper.class.getPackage().getImplementationVersion();

    return version != null ? version : "unknown";
  }
}
