package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.BodyContent;
import com.rollbar.api.payload.data.body.ExceptionInfo;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;

import java.util.ArrayList;
import java.util.List;

class MinBodyStrategy implements TruncationStrategy {
  private static final FramesStrategy FRAMES_STRATEGY = new FramesStrategy(1, 1);
  private static final int MAX_EXCEPTION_MSG_LENGTH = 255;

  @Override
  public TruncationResult<Payload> truncate(Payload payload) {
    if (payload == null || payload.getData() == null || payload.getData().getBody() == null) {
      return TruncationResult.none();
    }

    BodyContent content = payload.getData().getBody().getContents();

    if (content instanceof TraceChain) {
      return tryTruncateChain(payload, (TraceChain) content);
    } else if (content instanceof Trace) {
      return tryTruncateTrace(payload, (Trace) content);
    }

    return TruncationResult.none();
  }

  private TruncationResult<Payload> tryTruncateTrace(Payload payload, Trace trace) {
    TruncationResult<Trace> traceResult = truncateTrace(trace);
    if (traceResult.wasTruncated) {
      return payloadWithContent(payload, traceResult.value);
    }
    return TruncationResult.none();
  }

  private TruncationResult<Payload> tryTruncateChain(Payload payload, TraceChain chain) {
    boolean truncated = false;

    List<Trace> newTraces = new ArrayList<>();
    for (Trace trace : chain.getTraces()) {
      TruncationResult<Trace> traceResult = truncateTrace(trace);
      if (traceResult.wasTruncated) {
        newTraces.add(traceResult.value);
        truncated = true;
      } else {
        newTraces.add(trace);
      }
    }

    if (truncated) {
      return payloadWithContent(payload,
          new TraceChain.Builder(chain)
              .traces(newTraces)
              .build()
      );
    } else {
      return TruncationResult.none();
    }
  }

  private TruncationResult<Payload> payloadWithContent(Payload payload, BodyContent newContent) {
    Body body = new Body.Builder(payload.getData().getBody())
        .bodyContent(newContent)
        .build();

    Data data = new Data.Builder(payload.getData())
        .body(body)
        .build();

    Payload newPayload = new Payload.Builder(payload)
        .data(data)
        .build();

    return TruncationResult.truncated(newPayload);
  }

  private TruncationResult<Trace> truncateTrace(Trace trace) {
    boolean truncated = false;
    List<Frame> frames = trace.getFrames();
    if (trace.getFrames().size() > FRAMES_STRATEGY.totalFramesToKeep()) {
      frames = FRAMES_STRATEGY.truncateFrames(trace.getFrames());
      truncated = true;
    }

    ExceptionInfo exception = trace.getException();
    TruncationResult<ExceptionInfo> result = truncateException(exception);
    if (result.wasTruncated) {
      exception = result.value;
      truncated = true;
    }

    if (truncated) {
      Trace newTrace = new Trace.Builder(trace)
          .frames(frames)
          .exception(exception)
          .build();
      return TruncationResult.truncated(newTrace);
    } else {
      return TruncationResult.none();
    }
  }

  private TruncationResult<ExceptionInfo> truncateException(ExceptionInfo exception) {
    if (exception != null) {
      boolean truncated = false;

      String description = exception.getDescription();
      if (description != null) {
        description = null;
        truncated = true;
      }

      String message = exception.getMessage();
      if (message != null && message.length() > MAX_EXCEPTION_MSG_LENGTH) {
        message = message.substring(0, MAX_EXCEPTION_MSG_LENGTH);
        truncated = true;
      }

      if (truncated) {
        exception = new ExceptionInfo.Builder(exception)
            .description(description)
            .message(message)
            .build();
        return TruncationResult.truncated(exception);
      }
    }

    return TruncationResult.none();
  }
}
