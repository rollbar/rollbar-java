package com.rollbar.jvmti;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Covers {@link ThrowableCache#shouldCacheThrowable}, which the native agent consults
 * before doing any capture work.
 *
 * <p>The guard at its center (skip a throwable already cached with at least this many
 * frames) is what silently stopped working when the agent truncated the CacheFrame
 * array: {@code existing.length} was the truncated length rather than the real stack
 * depth, so {@code numFrames <= existing.length} never held and every rethrow
 * re-captured the same throwable.
 *
 * <p>Note {@link ThrowableCache} keeps static, additive app package state with no reset,
 * so the prefix registered here is deliberately distinctive and leaks to the rest of the
 * JVM. No other test's stack traces match it.
 */
public class ThrowableCacheTest {

  private static final String APP_PACKAGE = "com.rollbar.jvmti.testapp";
  private static final String APP_CLASS = APP_PACKAGE + ".Service";
  private static final String FRAMEWORK_CLASS = "com.example.fw.Dispatcher";

  static final class Frames {
    static void invoke() {
      // body irrelevant; only the reflected name is used
    }
  }

  // The "no app packages configured returns false" gate is deliberately not covered:
  // appPackages is static and additive with no reset, so such a test would depend on
  // running before every other test here, and JUnit does not order methods by source.
  // Covering it would mean adding a test-only reset to production code.

  @Test
  public void shouldCacheWhenAnAppFrameIsPresent() {
    ThrowableCache.addAppPackage(APP_PACKAGE);
    Throwable throwable = throwableWith(frameworkElement(1), appElement());

    assertTrue(ThrowableCache.shouldCacheThrowable(throwable, 2));
  }

  @Test
  public void shouldNotCacheWhenNoAppFrameIsPresent() {
    ThrowableCache.addAppPackage(APP_PACKAGE);
    Throwable throwable = throwableWith(frameworkElement(1), frameworkElement(2));

    assertFalse(ThrowableCache.shouldCacheThrowable(throwable, 2));
  }

  @Test
  public void shouldNotRecaptureWhenAlreadyCachedAtTheSameDepth() throws Exception {
    ThrowableCache.addAppPackage(APP_PACKAGE);
    int depth = 100;
    Throwable throwable = deepThrowable(depth);

    ThrowableCache.add(throwable, cacheFrames(depth));

    assertFalse("a throwable already cached at this depth must not be captured again",
        ThrowableCache.shouldCacheThrowable(throwable, depth));
  }

  @Test
  public void shouldRecaptureWhenTheCachedArrayIsShorterThanTheStack() throws Exception {
    // Characterizes the bug: a truncated cache leaves existing.length below the real
    // depth, so the guard never fires and the throwable is re-captured on every throw.
    // Kept to document why the agent must cache every frame, not as desired behavior.
    ThrowableCache.addAppPackage(APP_PACKAGE);
    int depth = 100;
    Throwable throwable = deepThrowable(depth);

    ThrowableCache.add(throwable, cacheFrames(64));

    assertTrue("a truncated cache defeats the re-capture guard",
        ThrowableCache.shouldCacheThrowable(throwable, depth));
  }

  private static CacheFrame[] cacheFrames(int count) throws Exception {
    Method invoke = Frames.class.getDeclaredMethod("invoke");
    CacheFrame[] frames = new CacheFrame[count];
    for (int i = 0; i < count; i++) {
      frames[i] = new CacheFrame(invoke, new LocalVariable[0]);
    }
    return frames;
  }

  private static Throwable deepThrowable(int depth) {
    StackTraceElement[] elements = new StackTraceElement[depth];
    for (int i = 0; i < depth - 1; i++) {
      elements[i] = frameworkElement(i);
    }
    elements[depth - 1] = appElement();
    return throwableWith(elements);
  }

  private static Throwable throwableWith(StackTraceElement... elements) {
    Throwable throwable = new IllegalStateException("boom");
    throwable.setStackTrace(elements);
    return throwable;
  }

  private static StackTraceElement appElement() {
    return new StackTraceElement(APP_CLASS, "invoke", "Service.java", 42);
  }

  private static StackTraceElement frameworkElement(int line) {
    return new StackTraceElement(FRAMEWORK_CLASS, "invoke", "Dispatcher.java", line);
  }
}
