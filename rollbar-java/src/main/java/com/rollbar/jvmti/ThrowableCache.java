package com.rollbar.jvmti;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * ThrowableCache is a mechanism for storing information from the native interface at the time
 * of an exception which can be queried later by the notifier for enhancing payloads.
 */
public final class ThrowableCache {
  private static Set<String> appPackages = new HashSet<>();

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
  public static void addAppPackage(String newAppPackage) {
    appPackages.add(newAppPackage);
  }
}
