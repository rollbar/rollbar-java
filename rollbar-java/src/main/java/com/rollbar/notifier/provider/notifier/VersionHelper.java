package com.rollbar.notifier.provider.notifier;

class VersionHelper {

  public String version() {
    return VersionHelper.class.getPackage().getImplementationVersion();
  }
}
