package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.payload.data.body.RollbarThread;
import com.rollbar.notifier.util.BodyFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadParser {
  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  private static final Pattern BEGIN_MANAGED_THREAD_RE =
      Pattern.compile("\"(.*)\" (.*) ?prio=(\\d+)\\s+tid=(\\d+)\\s*(.*)");

  private static final Pattern BEGIN_UNMANAGED_NATIVE_THREAD_RE =
      Pattern.compile("\"(.*)\" (.*) ?sysTid=(\\d+)");

  private static final Pattern NATIVE_RE =
      Pattern.compile(
          " *(?:native: )?#\\d+ \\S+ [0-9a-fA-F]+\\s+(.*?)\\s+\\((.*)\\+(\\d+)\\)(?: \\(.*\\))?");
  private static final Pattern NATIVE_NO_LOC_RE =
      Pattern.compile(
          " *(?:native: )?#\\d+ \\S+ [0-9a-fA-F]+\\s+(.*)\\s*\\(?(.*)\\)?(?: \\(.*\\))?");
  private static final Pattern JAVA_RE =
      Pattern.compile(" *at (?:(.+)\\.)?([^.]+)\\.([^.]+)\\((.*):([\\d-]+)\\)");
  private static final Pattern JNI_RE =
      Pattern.compile(" *at (?:(.+)\\.)?([^.]+)\\.([^.]+)\\(Native method\\)");
  private static final Pattern LOCKED_RE =
      Pattern.compile(" *- locked \\<([0x0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
  private static final Pattern SLEEPING_ON_RE =
      Pattern.compile(" *- sleeping on \\<([0x0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
  private static final Pattern WAITING_ON_RE =
      Pattern.compile(" *- waiting on \\<([0x0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
  private static final Pattern WAITING_TO_LOCK_RE =
      Pattern.compile(
          " *- waiting to lock \\<([0x0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
  private static final Pattern WAITING_TO_LOCK_HELD_RE =
      Pattern.compile(
          " *- waiting to lock \\<([0x0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)"
              + "(?: held by thread (\\d+))");
  private static final Pattern WAITING_TO_LOCK_UNKNOWN_RE =
      Pattern.compile(" *- waiting to lock an unknown object");
  private static final Pattern BLANK_RE = Pattern.compile("\\s+");

  public List<RollbarThread> parse(final  Lines lines) {
    Deque<RollbarThread> rollbarThreads = new ArrayDeque<>();
    final Matcher beginManagedThreadRe = BEGIN_MANAGED_THREAD_RE.matcher("");
    final Matcher beginUnmanagedNativeThreadRe = BEGIN_UNMANAGED_NATIVE_THREAD_RE.matcher("");

    while (lines.hasNext()) {
      Line line = lines.next();
      if (line == null) {
        LOGGER.warn("No line: Internal error while parsing thread dump");
        return new ArrayList<>(rollbarThreads);
      }
      final String text = line.getText();

      if (matches(beginManagedThreadRe, text) || matches(beginUnmanagedNativeThreadRe, text)) {
        lines.rewind();

        RollbarThread rollbarThread = parseThread(lines);
        if (rollbarThread != null) {
          if (rollbarThread.isMain()) {
            rollbarThreads.addFirst(rollbarThread);
          } else {
            rollbarThreads.addLast(rollbarThread);
          }
        }
      }
    }
    return new ArrayList<>(rollbarThreads);
  }

  private RollbarThread parseThread(final Lines lines) {
    String id = "";
    String name = "";
    String state = "";

    final Matcher beginManagedThreadRe = BEGIN_MANAGED_THREAD_RE.matcher("");
    final Matcher beginUnmanagedNativeThreadRe = BEGIN_UNMANAGED_NATIVE_THREAD_RE.matcher("");

    if (!lines.hasNext()) {
      return null;
    }
    final Line line = lines.next();
    if (line == null) {
      LOGGER.warn("Internal error while parsing thread dump");
      return null;
    }
    if (matches(beginManagedThreadRe, line.getText())) {
      Long threadId = getLong(beginManagedThreadRe, 4, null);
      if (threadId == null) {
        LOGGER.debug("No thread id in the dump, skipping thread");
        return null;
      }
      id = threadId.toString();
      name = beginManagedThreadRe.group(1);
      state = beginManagedThreadRe.group(5);

      if (state != null && state.contains(" ")) {
        state = state.substring(0, state.indexOf(' '));
      }
    } else if (matches(beginUnmanagedNativeThreadRe, line.getText())) {
      Long systemThreadId = getLong(beginUnmanagedNativeThreadRe, 3, null);
      if (systemThreadId == null) {
        LOGGER.debug("No system thread id in the dump, skipping thread");
        return null;
      }
      id = systemThreadId.toString();
      name = beginUnmanagedNativeThreadRe.group(1);
    }

    StackTrace stackTrace = parseStacktrace(lines);
    return new RollbarThread(
        name,
        id,
        "",
        state,
        new BodyFactory().from(stackTrace.getStackTraceElements())
    );
  }


  private StackTrace parseStacktrace(Lines lines) {
    final List<StackFrame> frames = new ArrayList<>();

    final Matcher nativeRe = NATIVE_RE.matcher("");
    final Matcher nativeNoLocRe = NATIVE_NO_LOC_RE.matcher("");
    final Matcher javaRe = JAVA_RE.matcher("");
    final Matcher jniRe = JNI_RE.matcher("");
    final Matcher blankRe = BLANK_RE.matcher("");

    while (lines.hasNext()) {
      final Line line = lines.next();
      if (line == null) {
        LOGGER.warn("Internal error while parsing thread dump, no line");
        break;
      }
      final String text = line.getText();
      if (matches(nativeRe, text)) {
        final StackFrame frame = new StackFrame();
        frame.setPackage(nativeRe.group(1));
        frame.setFunction(nativeRe.group(2));
        frame.setLineno(getInteger(nativeRe, 3, null));
        frames.add(frame);
      } else if (matches(nativeNoLocRe, text)) {
        final StackFrame frame = new StackFrame();
        frame.setPackage(nativeNoLocRe.group(1));
        frame.setFunction(nativeNoLocRe.group(2));
        frames.add(frame);
      } else if (matches(javaRe, text)) {
        final StackFrame frame = new StackFrame();
        final String packageName = javaRe.group(1);
        final String className = javaRe.group(2);
        final String module = String.format("%s.%s", packageName, className);
        frame.setModule(module);
        frame.setFunction(javaRe.group(3));
        frame.setFilename(javaRe.group(4));
        frame.setLineno(getUInteger(javaRe, 5, null));
        frames.add(frame);
      } else if (matches(jniRe, text)) {
        final StackFrame frame = new StackFrame();
        final String packageName = jniRe.group(1);
        final String className = jniRe.group(2);
        final String module = String.format("%s.%s", packageName, className);
        frame.setModule(module);
        frame.setFunction(jniRe.group(3));
        frames.add(frame);
      } else if (text.isEmpty() || matches(blankRe, text)) {
        break;
      }
    }

    Collections.reverse(frames);
    final StackTrace stackTrace = new StackTrace(frames);
    stackTrace.setSnapshot(true);
    return stackTrace;
  }

  private boolean matches(final  Matcher matcher, final  String text) {
    matcher.reset(text);
    return matcher.matches();
  }

  private Long getLong(
      final  Matcher matcher, final int group, final  Long defaultValue) {
    final String str = matcher.group(group);
    if (str == null || str.length() == 0) {
      return defaultValue;
    } else {
      return Long.parseLong(str);
    }
  }

  private Integer getInteger(
      final  Matcher matcher, final int group, final  Integer defaultValue) {
    final String str = matcher.group(group);
    if (str == null || str.isEmpty()) {
      return defaultValue;
    } else {
      return Integer.parseInt(str);
    }
  }

  private Integer getUInteger(
      final Matcher matcher, final int group, final  Integer defaultValue) {
    final String str = matcher.group(group);
    if (str == null || str.isEmpty()) {
      return defaultValue;
    } else {
      final Integer parsed = Integer.parseInt(str);
      return parsed >= 0 ? parsed : defaultValue;
    }
  }
}
