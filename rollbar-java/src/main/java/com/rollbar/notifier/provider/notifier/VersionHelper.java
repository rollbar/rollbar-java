package com.rollbar.notifier.provider.notifier;

class VersionHelper {

  /**
   * Get the current version of the `rollbar-java` notifier.
   *
   * <p>
   * When shading `rollbar-java` into a different jar, the version from our jar's manifest is lost,
   * and only the user's version is kept. Our classes become part of the user's jar, and
   * `VersionHelper.class.getPackage().getImplementationVersion()` returns the user's jar's
   * implementation version.
   * There several shading tools out there with different levels of support for resources, manifest
   * merging, etc... The only thing they all reliably support when they relocate a class is updating
   * class and method references present in bytecode form.
   * So rather than putting our version in a resource and hoping that we can still dynamically
   * reference it after relocation, we just create the VersionHelperResources class in Gradle, which
   * we know will still work after relocation since we're referencing it statically.
   * Obviously we keep the version in `rollbar-java`'s jar manifest, but we don't rely on it here.
   * </p>
   *
   * @return The version of the `rollbar-java` notifier currently loaded.
   */
  public String version() {
    return com.rollbar.notifier.provider.notifier.VersionHelperResources.getVersion();
  }
}
