package com.rollbar.android.anr.historical.stacktrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class StackTraceFactory {

  private static final int STACKTRACE_FRAME_LIMIT = 100;

  public StackTraceFactory(/*todo pass options?*/) {
  }
  
  public List<StackFrame> getStackFrames(
       final StackTraceElement[] elements, final boolean includeFrames) {
    List<StackFrame> StackFrames = null;

    if (elements != null && elements.length > 0) {
      StackFrames = new ArrayList<>();
      for (StackTraceElement item : elements) {
        if (item != null) {

          final String className = item.getClassName();
          if (!includeFrames && (className.startsWith("com.rollbar."))) {
            continue;
          }

          final StackFrame StackFrame = new StackFrame();
          StackFrame.setInApp(isInApp(className));
          StackFrame.setModule(className);
          StackFrame.setFunction(item.getMethodName());
          StackFrame.setFilename(item.getFileName());
          if (item.getLineNumber() >= 0) {
            StackFrame.setLineno(item.getLineNumber());
          }
          StackFrame.setNative(item.isNativeMethod());
          StackFrames.add(StackFrame);

          if (StackFrames.size() >= STACKTRACE_FRAME_LIMIT) {
            break;
          }
        }
      }
      Collections.reverse(StackFrames);
    }

    return StackFrames;
  }

  /**
   * Returns if the className is InApp or not.
   *
   * @param className the className
   * @return true if it is or false otherwise
   */
  
  public Boolean isInApp(final  String className) {
    if (className == null || className.isEmpty()) {
      return true;
    }
/*
    final List<String> inAppIncludes = options.getInAppIncludes();
    for (String include : inAppIncludes) {
      if (className.startsWith(include)) {
        return true;
      }
    }

 */
/*
    final List<String> inAppExcludes = options.getInAppExcludes();
    for (String exclude : inAppExcludes) {
      if (className.startsWith(exclude)) {
        return false;
      }
    }

 */

    return null;
  }

  /**
   * Returns the call stack leading to the exception, including in-app frames and excluding rollbar
   * and system frames.
   *
   * @param exception an exception to get the call stack to
   * @return a list of rollbar stack frames leading to the exception
   */

  List<StackFrame> getInAppCallStack(final  Throwable exception) {
    final StackTraceElement[] stacktrace = exception.getStackTrace();
    final List<StackFrame> frames = getStackFrames(stacktrace, false);
    if (frames == null) {
      return Collections.emptyList();
    }
/*
    final List<StackFrame> inAppFrames =
        CollectionUtils.filterListEntries(frames, (frame) -> Boolean.TRUE.equals(frame.isInApp()));

    if (!inAppFrames.isEmpty()) {
      return inAppFrames;
    }

    // if inAppFrames is empty, most likely we're operating over an obfuscated app, just trying to
    // fallback to all the frames that are not system frames
    return CollectionUtils.filterListEntries(
        frames,
        (frame) -> {
          final String module = frame.getModule();
          boolean isSystemFrame = false;
          if (module != null) {
            isSystemFrame =
                module.startsWith("sun.")
                    || module.startsWith("java.")
                    || module.startsWith("android.")
                    || module.startsWith("com.android.");
          }
          return !isSystemFrame;
        });
        todo crb
 */
    return Collections.emptyList();
  }

  public List<StackFrame> getInAppCallStack() {
    return getInAppCallStack(new Exception());
  }
}
