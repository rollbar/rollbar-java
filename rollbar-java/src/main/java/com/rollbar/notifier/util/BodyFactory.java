package com.rollbar.notifier.util;

import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.BodyContent;
import com.rollbar.api.payload.data.body.ExceptionInfo;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Group;
import com.rollbar.api.payload.data.body.Message;
import com.rollbar.api.payload.data.body.RollbarThread;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import com.rollbar.jvmti.CacheFrame;
import com.rollbar.jvmti.ThrowableCache;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Body factory helper to build the proper body depending on the throwable or the description.
 */
public class BodyFactory {

  /**
   * Builds the body for the throwable and description supplied.
   *
   * @param throwable   the throwable.
   * @param description the description.
   * @return the body.
   * @deprecated Replaced by {@link #from(ThrowableWrapper, String)}.
   */
  @Deprecated
  public Body from(Throwable throwable, String description) {
    if (throwable == null) {
      return new Body.Builder().bodyContent(message(description)).build();
    }
    return from(new RollbarThrowableWrapper(throwable), description);
  }

  /**
   * Builds the body from the {@link ThrowableWrapper throwableWrapper} and the description
   * supplied.
   *
   * @param throwableWrapper the throwable proxy.
   * @param description      the description.
   * @return the body.
   */
  public Body from(ThrowableWrapper throwableWrapper, String description) {
    Body.Builder builder = new Body.Builder();
    return from(throwableWrapper, description, builder);
  }

  /**
   * Builds the body from the {@link ThrowableWrapper throwableWrapper}, the description
   * supplied and telemetry events.
   *
   * @param throwableWrapper the throwable proxy.
   * @param description      the description.
   * @param telemetryEvents  the telemetry events.
   * @return the body.
   */
  public Body from(
      ThrowableWrapper throwableWrapper,
      String description,
      List<TelemetryEvent> telemetryEvents
  ) {
    Body.Builder builder = new Body.Builder().telemetryEvents(telemetryEvents);
    return from(throwableWrapper, description, builder);
  }

  /**
   * Builds a RollbarThread from a Thread.
   *
   * @param thread the thread.
   * @return the RollbarThread.
   */
  public RollbarThread from(
      Thread thread
  ) {
    if (thread == null) {
      return null;
    }
    TraceChain traceChain = traceChain(thread.getStackTrace());
    return new RollbarThread(thread, new Group(traceChain));
  }

  /**
   * Builds a Group from an Array of StackTraceElement.
   *
   * @param stackTraceElements the stack trace elements.
   * @return the Group.
   */
  public Group from(
      StackTraceElement[] stackTraceElements
  ) {
    if (stackTraceElements == null) {
      return null;
    }
    TraceChain traceChain = traceChain(stackTraceElements);
    return new Group(traceChain);
  }

  private Body from(
      ThrowableWrapper throwableWrapper,
      String description,
      Body.Builder builder
  ) {
    return builder
      .bodyContent(makeBodyContent(throwableWrapper, description))
      .rollbarThreads(makeRollbarThreads(throwableWrapper, description))
      .build();
  }

  private List<RollbarThread> makeRollbarThreads(
      ThrowableWrapper throwableWrapper,
      String description
  ) {
    if (throwableWrapper == null) {
      return null;
    }

    List<RollbarThread> wrapperRollbarThreads = throwableWrapper.getRollbarThreads();
    if (wrapperRollbarThreads != null && !wrapperRollbarThreads.isEmpty()) {
      return wrapperRollbarThreads;
    }

    Map<Thread, StackTraceElement[]> allStackTraces = throwableWrapper.getAllStackTraces();
    if (allStackTraces == null) {
      return null;
    }

    RollbarThread rollbarThread = throwableWrapper.getRollbarThread();
    if (rollbarThread == null) {
      return null;
    }

    ArrayList<RollbarThread> rollbarThreads = new ArrayList<>();
    rollbarThreads.add(updateInitialRollbarThread(rollbarThread, throwableWrapper, description));
    return addOtherThreads(rollbarThreads, allStackTraces);
  }

  private RollbarThread updateInitialRollbarThread(
      RollbarThread rollbarThread,
      ThrowableWrapper throwableWrapper,
      String description
  ) {
    TraceChain traceChain = traceChain(throwableWrapper, description);
    return new RollbarThread.Builder(rollbarThread).group(new Group(traceChain)).build();
  }

  private ArrayList<RollbarThread> addOtherThreads(
      ArrayList<RollbarThread> rollbarThreads,
      Map<Thread, StackTraceElement[]> allStackTraces
  ) {
    for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
      TraceChain traceChain = traceChain(entry.getValue());
      RollbarThread rollbarThread = new RollbarThread(entry.getKey(), new Group(traceChain));
      rollbarThreads.add(rollbarThread);
    }
    return rollbarThreads;
  }

  private BodyContent makeBodyContent(ThrowableWrapper throwableWrapper, String description) {
    if (throwableWrapper == null) {
      return message(description);
    }

    if (throwableWrapper.getCause() == null) {
      return trace(throwableWrapper, description);
    }

    return traceChain(throwableWrapper, description);
  }

  private static Message message(String description) {
    return new Message.Builder()
      .body(description)
      .build();
  }

  private static Trace trace(ThrowableWrapper throwableWrapper, String description) {
    return new Trace.Builder()
      .frames(frames(throwableWrapper))
      .exception(info(throwableWrapper, description))
      .build();
  }

  private TraceChain traceChain(StackTraceElement[] stackTraceElements) {
    List<Frame> frames = frames(stackTraceElements);
    Trace trace = new Trace.Builder().frames(frames).build();
    ArrayList<Trace> chain = new ArrayList<>();
    chain.add(trace);
    return new TraceChain.Builder().traces(chain).build();
  }

  private static TraceChain traceChain(ThrowableWrapper throwableWrapper, String description) {
    ArrayList<Trace> chain = new ArrayList<>();
    do {
      chain.add(trace(throwableWrapper, description));
      description = null;
      throwableWrapper = throwableWrapper.getCause();
    } while (throwableWrapper != null);
    return new TraceChain.Builder()
      .traces(chain)
      .build();
  }

  private static List<Frame> frames(ThrowableWrapper throwableWrapper) {
    StackTraceElement[] elements = throwableWrapper.getStackTrace();
    CacheFrame[] cachedFrames = ThrowableCache.get(throwableWrapper.getThrowable());

    // The native agent caches one CacheFrame per stack frame, so the arrays line up
    // positionally and no name matching is needed. Taking this path also means
    // getMethod() is never queried for a name, which matters because the agent stores a
    // Constructor - not a Method - for every <init> frame. See methodNameOf below.
    boolean aligned = cachedFrames != null && cachedFrames.length == elements.length;

    int j = 0;
    if (cachedFrames != null) {
      j = cachedFrames.length - 1;
    }

    ArrayList<Frame> result = new ArrayList<>();
    for (int i = elements.length - 1; i >= 0; i--, j--) {
      StackTraceElement element = elements[i];
      Map<String, Object> locals = null;
      if (cachedFrames != null) {
        if (!aligned) {
          // Fallback for caches that do not span the whole stack, as produced by older
          // agents: resync by method name walking both arrays from the bottom.
          while (j >= 0 && !methodNameOf(methodOf(cachedFrames[j]))
              .equals(element.getMethodName())) {
            j--;
          }
        }
        if (j >= 0 && cachedFrames[j] != null) {
          locals = cachedFrames[j].getLocals();
        }
      }
      result.add(frame(element, locals));
    }

    return result;
  }

  private static List<Frame> frames(StackTraceElement[] stackTraceElements) {

    ArrayList<Frame> result = new ArrayList<>();
    for (int i = stackTraceElements.length - 1; i >= 0; i--) {
      result.add(frame(stackTraceElements[i], null));
    }

    return result;
  }

  private static Object methodOf(CacheFrame cachedFrame) {
    return cachedFrame == null ? null : cachedFrame.getMethod();
  }

  /**
   * The name a {@link StackTraceElement} would report for this frame's method.
   *
   * <p>{@code CacheFrame.getMethod()} is declared to return a {@link Method}, but the native
   * agent fills that field through {@code JNI ToReflectedMethod}, which yields a
   * {@link Constructor} for every {@code <init>} frame, and JNI does not type check what it
   * stores. {@code Constructor.getName()} is the declaring class name, never {@code "<init>"},
   * so a raw {@code getName()} here would never match the stack trace element and would strand
   * the resync walk.
   */
  static String methodNameOf(Object method) {
    if (method == null) {
      return "";
    }
    if (method instanceof Constructor) {
      return "<init>";
    }
    return ((Method) method).getName();
  }

  private static ExceptionInfo info(ThrowableWrapper throwableWrapper, String description) {
    String className = throwableWrapper.getClassName();
    String message = throwableWrapper.getMessage();
    return new ExceptionInfo.Builder()
      .className(className)
      .message(message)
      .description(description)
      .build();
  }

  private static Frame frame(StackTraceElement element, Map<String, Object> locals) {
    String filename = element.getFileName();
    Integer lineNumber = element.getLineNumber();
    String method = element.getMethodName();
    String className = element.getClassName();

    return new Frame.Builder()
      .filename(filename)
      .lineNumber(lineNumber)
      .method(method)
      .className(className)
      .locals(locals)
      .build();
  }
}
