package com.rollbar.android.anr.historical.stacktrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadDumpParser {
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


  private final boolean isBackground;

  public ThreadDumpParser(final boolean isBackground) {
    this.isBackground = isBackground;
  }


  public List<RollbarThread> parse(final  Lines lines) {
    final List<RollbarThread> rollbarThreads = new ArrayList<>();

    final Matcher beginManagedThreadRe = BEGIN_MANAGED_THREAD_RE.matcher("");
    final Matcher beginUnmanagedNativeThreadRe = BEGIN_UNMANAGED_NATIVE_THREAD_RE.matcher("");

    while (lines.hasNext()) {
      Line line = lines.next();
      if (line == null) {
        LOGGER.warn("No line: Internal error while parsing thread dump");
        return rollbarThreads;
      }
      final String text = line.text;

      if (matches(beginManagedThreadRe, text) || matches(beginUnmanagedNativeThreadRe, text)) {
        lines.rewind();

        final RollbarThread rollbarThread = parseThread(lines);
        if (rollbarThread != null) {
          rollbarThreads.add(rollbarThread);
        }
      }
    }
    return rollbarThreads;
  }

  private RollbarThread parseThread(final Lines lines) {
    final RollbarThread RollbarThread = new RollbarThread();

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
    if (matches(beginManagedThreadRe, line.text)) {
      Long threadId = getLong(beginManagedThreadRe, 4, null);
      if (threadId == null) {
        LOGGER.debug("No thread id in the dump, skipping thread");
        return null;
      }
      RollbarThread.setId(threadId);
      RollbarThread.setName(beginManagedThreadRe.group(1));
      final String state = beginManagedThreadRe.group(5);

      if (state != null) {
        if (state.contains(" ")) {
          RollbarThread.setState(state.substring(0, state.indexOf(' ')));
        } else {
          RollbarThread.setState(state);
        }
      }
    } else if (matches(beginUnmanagedNativeThreadRe, line.text)) {
      Long systemThreadId = getLong(beginUnmanagedNativeThreadRe, 3, null);
      if (systemThreadId == null) {
        LOGGER.debug("No thread id in the dump, skipping thread");
        return null;
      }
      RollbarThread.setId(systemThreadId);
      RollbarThread.setName(beginUnmanagedNativeThreadRe.group(1));
    }

    final String threadName = RollbarThread.getName();
    if (threadName != null) {
      boolean isMain = threadName.equals("main");
      RollbarThread.setMain(isMain);
      RollbarThread.setCrashed(isMain);
      RollbarThread.setCurrent(isMain && !isBackground);
    }

    final StackTrace stackTrace = parseStacktrace(lines, RollbarThread);
    RollbarThread.setStacktrace(stackTrace);
    return RollbarThread;
  }


  private StackTrace parseStacktrace(
      final  Lines lines, final RollbarThread rollbarThread) {
    final List<StackFrame> frames = new ArrayList<>();
    StackFrame lastJavaFrame = null;

    final Matcher nativeRe = NATIVE_RE.matcher("");
    final Matcher nativeNoLocRe = NATIVE_NO_LOC_RE.matcher("");
    final Matcher javaRe = JAVA_RE.matcher("");
    final Matcher jniRe = JNI_RE.matcher("");
    final Matcher lockedRe = LOCKED_RE.matcher("");
    final Matcher waitingOnRe = WAITING_ON_RE.matcher("");
    final Matcher sleepingOnRe = SLEEPING_ON_RE.matcher("");
    final Matcher waitingToLockHeldRe = WAITING_TO_LOCK_HELD_RE.matcher("");
    final Matcher waitingToLockRe = WAITING_TO_LOCK_RE.matcher("");
    final Matcher waitingToLockUnknownRe = WAITING_TO_LOCK_UNKNOWN_RE.matcher("");
    final Matcher blankRe = BLANK_RE.matcher("");

    while (lines.hasNext()) {
      final Line line = lines.next();
      if (line == null) {
        LOGGER.warn("Internal error while parsing thread dump");
        break;
      }
      final String text = line.text;
      if (matches(nativeRe, text)) {
        final StackFrame frame = new StackFrame();
        frame.setPackage(nativeRe.group(1));
        frame.setFunction(nativeRe.group(2));
        frame.setLineno(getInteger(nativeRe, 3, null));
        frames.add(frame);
        lastJavaFrame = null;
      } else if (matches(nativeNoLocRe, text)) {
        final StackFrame frame = new StackFrame();
        frame.setPackage(nativeNoLocRe.group(1));
        frame.setFunction(nativeNoLocRe.group(2));
        frames.add(frame);
        lastJavaFrame = null;
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
        lastJavaFrame = frame;
      } else if (matches(jniRe, text)) {
        final StackFrame frame = new StackFrame();
        final String packageName = jniRe.group(1);
        final String className = jniRe.group(2);
        final String module = String.format("%s.%s", packageName, className);
        frame.setModule(module);
        frame.setFunction(jniRe.group(3));
        frames.add(frame);
        lastJavaFrame = frame;
      } else if (matches(lockedRe, text)) {
        if (lastJavaFrame != null) {
          final LockReason lock = new LockReason();
          lock.setType(LockReason.LOCKED);
          lock.setAddress(lockedRe.group(1));
          lock.setPackageName(lockedRe.group(2));
          lock.setClassName(lockedRe.group(3));
          lastJavaFrame.setLock(lock);
          combineThreadLocks(rollbarThread, lock);
        }
      } else if (matches(waitingOnRe, text)) {
        if (lastJavaFrame != null) {
          final LockReason lock = new LockReason();
          lock.setType(LockReason.WAITING);
          lock.setAddress(waitingOnRe.group(1));
          lock.setPackageName(waitingOnRe.group(2));
          lock.setClassName(waitingOnRe.group(3));
          lastJavaFrame.setLock(lock);
          combineThreadLocks(rollbarThread, lock);
        }
      } else if (matches(sleepingOnRe, text)) {
        if (lastJavaFrame != null) {
          final LockReason lock = new LockReason();
          lock.setType(LockReason.SLEEPING);
          lock.setAddress(sleepingOnRe.group(1));
          lock.setPackageName(sleepingOnRe.group(2));
          lock.setClassName(sleepingOnRe.group(3));
          lastJavaFrame.setLock(lock);
          combineThreadLocks(rollbarThread, lock);
        }
      } else if (matches(waitingToLockHeldRe, text)) {
        if (lastJavaFrame != null) {
          final LockReason lock = new LockReason();
          lock.setType(LockReason.BLOCKED);
          lock.setAddress(waitingToLockHeldRe.group(1));
          lock.setPackageName(waitingToLockHeldRe.group(2));
          lock.setClassName(waitingToLockHeldRe.group(3));
          lock.setThreadId(getLong(waitingToLockHeldRe, 4, null));
          lastJavaFrame.setLock(lock);
          combineThreadLocks(rollbarThread, lock);
        }
      } else if (matches(waitingToLockRe, text)) {
        if (lastJavaFrame != null) {
          final LockReason lock = new LockReason();
          lock.setType(LockReason.BLOCKED);
          lock.setAddress(waitingToLockRe.group(1));
          lock.setPackageName(waitingToLockRe.group(2));
          lock.setClassName(waitingToLockRe.group(3));
          lastJavaFrame.setLock(lock);
          combineThreadLocks(rollbarThread, lock);
        }
      } else if (matches(waitingToLockUnknownRe, text)) {
        if (lastJavaFrame != null) {
          final LockReason lock = new LockReason();
          lock.setType(LockReason.BLOCKED);
          lastJavaFrame.setLock(lock);
          combineThreadLocks(rollbarThread, lock);
        }
      } else if (text.length() == 0 || matches(blankRe, text)) {
        break;
      }
    }

    Collections.reverse(frames);//Todo review later
    final StackTrace stackTrace = new StackTrace(frames);
    stackTrace.setSnapshot(true);
    return stackTrace;
  }

  private boolean matches(final  Matcher matcher, final  String text) {
    matcher.reset(text);
    return matcher.matches();
  }

  private void combineThreadLocks(
      final RollbarThread rollbarThread, final  LockReason lockReason) {
    Map<String, LockReason> heldLocks = rollbarThread.getHeldLocks();
    if (heldLocks == null) {
      heldLocks = new HashMap<>();
    }
    final LockReason prev = heldLocks.get(lockReason.getAddress());
    if (prev != null) {
      prev.setType(Math.max(prev.getType(), lockReason.getType()));
    } else {
      heldLocks.put(lockReason.getAddress(), new LockReason(lockReason));
    }
    rollbarThread.setHeldLocks(heldLocks);
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
