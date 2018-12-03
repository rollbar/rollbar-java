package com.rollbar.jvmti;

import java.util.*;

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

  public static void add(Throwable throwable, CacheFrame[] frames) {
    Map<Throwable, CacheFrame[]> weakMap = cache.get();
    weakMap.put(throwable, frames);
  }

  public static CacheFrame[] get(Throwable throwable) {
    if (throwable == null) {
      return null;
    }
    Map<Throwable, CacheFrame[]> weakMap = cache.get();
    return weakMap.get(throwable);
  }

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

  public static void addAppPackage(String newAppPackage) {
    appPackages.add(newAppPackage);
  }
}
