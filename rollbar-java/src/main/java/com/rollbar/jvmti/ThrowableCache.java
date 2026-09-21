package com.rollbar.jvmti;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * ThrowableCache is a mechanism for storing information from the native interface at the time
 * of an exception which can be queried later by the notifier for enhancing payloads.
 */
public final class ThrowableCache {
  /**
   * Copy-on-write so the native agent, which reads this from arbitrary throwing threads, never
   * observes a partially updated set.
   */
  private static volatile Set<String> appPackages = Collections.emptySet();

  /**
   * Incremented whenever {@link #appPackages} changes. The native agent reads this field
   * directly via JNI {@code GetStaticIntField} on every exception so it can skip all further
   * work while it is zero, and refresh its own copy of the prefixes when it changes. Zero means
   * "no app packages configured", in which case {@link #shouldCacheThrowable} can only return
   * false. Do not rename or change the type without updating native-agent/src/filter.rs.
   */
  static volatile int appPackagesVersion = 0;

  private static ThreadLocal<WeakHashMap<Throwable, CacheFrame[]>> cache =
      new ThreadLocal<WeakHashMap<Throwable, CacheFrame[]>>() {
        @Override
        protected WeakHashMap<Throwable, CacheFrame[]> initialValue() {
          return new WeakHashMap<>();
        }
      };

  private ThrowableCache() {}

  /**
   * Add a list of frames with extra information to the cache associated to this particular
   * throwable.
   *
   * @param throwable a throwable to use as a cache key.
   * @param frames frames associated with this throwable.
   */
  public static void add(Throwable throwable, CacheFrame[] frames) {
    Map<Throwable, CacheFrame[]> weakMap = cache.get();
    weakMap.put(throwable, frames);
  }

  /**
   * Get the cached frames associated with the given throwable.
   *
   * @param throwable a throwable to use as a cache key.
   * @return the list of frames previously cached or null.
   */
  public static CacheFrame[] get(Throwable throwable) {
    if (throwable == null) {
      return null;
    }
    Map<Throwable, CacheFrame[]> weakMap = cache.get();
    return weakMap.get(throwable);
  }

  /**
   * Whether or not we should cache this throwable which has a particular number of frames in
   * its stacktrace.
   *
   * @param throwable the throwable we might want to cache.
   * @param numFrames the number of frames in the stacktrace.
   * @return true if we should gather info about this throwable and cache it.
   */
  public static boolean shouldCacheThrowable(Throwable throwable, int numFrames) {
    if (appPackages.isEmpty()) {
      return false;
    }

    Map<Throwable, CacheFrame[]> weakMap = cache.get();
    CacheFrame[] existing = weakMap.get(throwable);
    if (existing != null && numFrames <= existing.length) {
      return false;
    }

    for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
      for (String appFrame : appPackages) {
        if (stackTraceElement.getClassName().startsWith(appFrame)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Add a string representing a package prefix to check against class names in stack traces to
   * determine whether to cache throwables or not.
   *
   * @param newAppPackage a string to add to the set of packages in your app.
   */
  public static synchronized void addAppPackage(String newAppPackage) {
    Set<String> updated = new HashSet<>(appPackages);
    updated.add(newAppPackage);
    // Publish the set before bumping the version: a reader that sees the new version is then
    // guaranteed to see the new set.
    appPackages = Collections.unmodifiableSet(updated);
    appPackagesVersion++;
  }

  /**
   * Snapshot of the configured app packages, read by the native agent so it can apply the same
   * prefix filter without calling back into Java for every exception.
   *
   * @return the currently configured package prefixes.
   */
  static String[] appPackagesSnapshot() {
    Set<String> current = appPackages;
    return current.toArray(new String[0]);
  }
}
