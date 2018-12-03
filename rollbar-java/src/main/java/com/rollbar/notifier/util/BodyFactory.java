package com.rollbar.notifier.util;

import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.ExceptionInfo;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Message;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import com.rollbar.jvmti.ThrowableCache;
import com.rollbar.jvmti.CacheFrame;
import com.rollbar.notifier.wrapper.RollbarThrowableWrapper;
import com.rollbar.notifier.wrapper.ThrowableWrapper;
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
   * @param throwable the throwable.
   * @param description the description.
   * @return the body.
   *
   * @deprecated  Replaced by {@link #from(ThrowableWrapper, String)}.
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
   * @param description the description.
   * @return the body.
   */
  public Body from(ThrowableWrapper throwableWrapper, String description) {
    Body.Builder builder = new Body.Builder();
    if (throwableWrapper == null) {
      return builder.bodyContent(message(description)).build();
    }
    if (throwableWrapper.getCause() == null) {
      return builder.bodyContent(trace(throwableWrapper, description)).build();
    }
    return builder.bodyContent(traceChain(throwableWrapper, description)).build();
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
    int j = 0;
    if (cachedFrames != null) {
      j = cachedFrames.length - 1;
    }

    ArrayList<Frame> result = new ArrayList<>();
    for (int i = elements.length - 1; i >= 0; i--, j--) {
      StackTraceElement element = elements[i];
      Map<String, Object> locals = null;
      if (cachedFrames != null) {
        while (j >= 0 && !cachedFrames[j].getMethod().getName().equals(element.getMethodName())) {
          j--;
        }
        if (j >= 0) {
          locals = cachedFrames[j].getLocals();
        }
      }
      result.add(frame(element, locals));
    }

    return result;
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
