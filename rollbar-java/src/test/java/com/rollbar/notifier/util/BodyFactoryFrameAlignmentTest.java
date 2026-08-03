package com.rollbar.notifier.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.jvmti.CacheFrame;
import com.rollbar.jvmti.LocalVariable;
import com.rollbar.jvmti.ThrowableCache;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Pins down how {@link BodyFactory} aligns the {@link CacheFrame} array supplied by the
 * native agent against the throwable's own stack trace.
 *
 * <p>The alignment walks both arrays from the bottom and resyncs by method <em>name</em>,
 * which only holds if the cached array spans the same frames as the stack trace. An
 * agent that cached a subset taken from the top of the stack would align against the
 * wrong region: locals would normally vanish, and could be attached to an unrelated
 * frame whenever method names collide - and names like {@code invoke} recur constantly
 * in framework stacks.
 *
 * <p>These tests need no native agent; they populate the cache directly.
 */
public class BodyFactoryFrameAlignmentTest {

  private static final String APP_CLASS = "com.example.app.Service";
  private static final String FRAMEWORK_CLASS = "com.example.fw.Dispatcher";
  private static final String ENTRY_CLASS = "com.example.app.Main";

  private BodyFactory sut;

  @Before
  public void setUp() {
    sut = new BodyFactory();
  }

  /**
   * Shares its name with {@link Framework#invoke} on purpose.
   */
  static final class App {
    static void invoke() {
      // body irrelevant; only the reflected name is used
    }

    static void entryPoint() {
      // body irrelevant; only the reflected name is used
    }
  }

  static final class Framework {
    static void invoke() {
      // body irrelevant; only the reflected name is used
    }
  }

  @Test
  public void shouldAttachLocalsToTheOwningFrameWhenNamesCollide() throws Exception {
    // Stack, top first: many framework `invoke` frames above a single app `invoke`.
    StackTraceElement[] elements = {
        frameworkElement(30),
        frameworkElement(20),
        frameworkElement(10),
        new StackTraceElement(APP_CLASS, "invoke", "Service.java", 42),
        new StackTraceElement(ENTRY_CLASS, "entryPoint", "Main.java", 7),
    };

    Method frameworkInvoke = Framework.class.getDeclaredMethod("invoke");
    Method appInvoke = App.class.getDeclaredMethod("invoke");
    Method entryPoint = App.class.getDeclaredMethod("entryPoint");

    // Full-length cache, 1:1 with the stack, locals only on the app frame. This is
    // what the agent must produce for the alignment to hold.
    CacheFrame[] cached = {
        new CacheFrame(frameworkInvoke, new LocalVariable[0]),
        new CacheFrame(frameworkInvoke, new LocalVariable[0]),
        new CacheFrame(frameworkInvoke, new LocalVariable[0]),
        new CacheFrame(appInvoke, new LocalVariable[] {new LocalVariable("appMarker", 777)}),
        new CacheFrame(entryPoint, new LocalVariable[0]),
    };

    List<Frame> frames = framesFor(elements, cached);

    assertThat(localsOf(frames, APP_CLASS), hasEntry("appMarker", (Object) 777));

    Map<String, Object> frameworkLocals = localsOf(frames, FRAMEWORK_CLASS);
    assertTrue("framework frame must not borrow the app frame's locals",
        frameworkLocals == null || frameworkLocals.isEmpty());
  }

  @Test
  public void shouldKeepLocalsOnDeepStacksBeyondAnyCaptureCap() throws Exception {
    // Deeper than the agent's former 64-frame capture cap, with the only app frame at
    // the very bottom - the shape that silently lost locals.
    int frameworkFrames = 200;
    StackTraceElement[] elements = new StackTraceElement[frameworkFrames + 1];
    for (int i = 0; i < frameworkFrames; i++) {
      elements[i] = frameworkElement(i);
    }
    elements[frameworkFrames] =
        new StackTraceElement(APP_CLASS, "invoke", "Service.java", 42);

    Method frameworkInvoke = Framework.class.getDeclaredMethod("invoke");
    Method appInvoke = App.class.getDeclaredMethod("invoke");

    CacheFrame[] cached = new CacheFrame[elements.length];
    for (int i = 0; i < frameworkFrames; i++) {
      cached[i] = new CacheFrame(frameworkInvoke, new LocalVariable[0]);
    }
    cached[frameworkFrames] =
        new CacheFrame(appInvoke, new LocalVariable[] {new LocalVariable("deepMarker", 99)});

    List<Frame> frames = framesFor(elements, cached);

    assertThat(frames.size(), is(equalTo(elements.length)));
    assertThat(localsOf(frames, APP_CLASS), hasEntry("deepMarker", (Object) 99));
  }

  /**
   * Characterises why the agent must cache every frame. Feeding a cache truncated to
   * the top of a deeper stack - what the agent used to produce - leaves the alignment
   * walk searching the wrong region, so the app frame's locals are lost. Kept as an
   * executable statement of the constraint, not as desirable behaviour.
   */
  @Test
  public void truncatedCacheLosesLocalsSoTheAgentMustCaptureEveryFrame() throws Exception {
    int frameworkFrames = 200;
    StackTraceElement[] elements = new StackTraceElement[frameworkFrames + 1];
    for (int i = 0; i < frameworkFrames; i++) {
      elements[i] = frameworkElement(i);
    }
    elements[frameworkFrames] =
        new StackTraceElement(APP_CLASS, "invoke", "Service.java", 42);

    Method frameworkInvoke = Framework.class.getDeclaredMethod("invoke");

    // Only the top 64 frames, so the app frame at the bottom is absent entirely.
    CacheFrame[] truncated = new CacheFrame[64];
    for (int i = 0; i < truncated.length; i++) {
      truncated[i] = new CacheFrame(frameworkInvoke, new LocalVariable[0]);
    }

    List<Frame> frames = framesFor(elements, truncated);

    Map<String, Object> appLocals = localsOf(frames, APP_CLASS);
    assertTrue("a truncated cache cannot carry the app frame's locals",
        appLocals == null || appLocals.isEmpty());
  }

  @Test
  public void shouldProduceFramesWhenNothingIsCached() {
    StackTraceElement[] elements = {
        new StackTraceElement(APP_CLASS, "invoke", "Service.java", 42),
    };

    Throwable throwable = new IllegalStateException("boom");
    throwable.setStackTrace(elements);

    List<Frame> frames = tracedFrames(throwable);

    assertThat(frames.size(), is(equalTo(1)));
    assertThat(frames.get(0).getLocals(), is(nullValue()));
  }

  private static StackTraceElement frameworkElement(int line) {
    return new StackTraceElement(FRAMEWORK_CLASS, "invoke", "Dispatcher.java", line);
  }

  private List<Frame> framesFor(StackTraceElement[] elements, CacheFrame[] cached) {
    Throwable throwable = new IllegalStateException("boom");
    throwable.setStackTrace(elements);
    ThrowableCache.add(throwable, cached);
    return tracedFrames(throwable);
  }

  private List<Frame> tracedFrames(Throwable throwable) {
    Body body = sut.from(throwable, null);
    return ((Trace) body.getContents()).getFrames();
  }

  private static Map<String, Object> localsOf(List<Frame> frames, String className) {
    for (Frame frame : frames) {
      if (className.equals(frame.getClassName())) {
        return frame.getLocals();
      }
    }
    throw new AssertionError("no frame for " + className);
  }
}
