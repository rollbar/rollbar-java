package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.BodyContent;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;

import java.util.ArrayList;
import java.util.List;

class FramesStrategy implements TruncationStrategy {
  private final int headFrameCount;
  private final int tailFrameCount;

  public FramesStrategy() {
    this(10, 10);
  }

  public FramesStrategy(int headFrameCount, int tailFrameCount) {
    this.headFrameCount = headFrameCount;
    this.tailFrameCount = tailFrameCount;
  }

  @Override
  public TruncationResult<Payload> truncate(Payload payload) {
    if (payload == null || payload.getData() == null || payload.getData().getBody() == null) {
      return TruncationResult.none();
    }

    Body body = payload.getData().getBody();
    BodyContent content = body.getContents();

    if (content instanceof Trace) {
      return mapResult(payload, truncateTrace((Trace) content));
    } else if (content instanceof TraceChain) {
      return mapResult(payload, truncateTraceChain((TraceChain) content));
    }

    return TruncationResult.none();
  }

  private TruncationResult<TraceChain> truncateTraceChain(TraceChain chain) {
    boolean truncated = false;

    ArrayList<Trace> updated = new ArrayList<>();
    for (Trace trace: chain.getTraces()) {
      TruncationResult<Trace> result = truncateTrace(trace);
      if (result.wasTruncated) {
        updated.add(result.value);
      } else {
        updated.add(trace);
      }

      truncated |= result.wasTruncated;
    }

    if (truncated) {
      return TruncationResult.truncated(new TraceChain.Builder(chain).traces(updated).build());
    } else {
      return TruncationResult.none();
    }
  }

  private TruncationResult<Trace> truncateTrace(Trace trace) {
    List<Frame> frames = trace.getFrames();

    if (frames.size() <= totalFramesToKeep()) {
      return TruncationResult.none();
    }

    List<Frame> updatedFrames = truncateFrames(frames);

    return TruncationResult.truncated(
        new Trace.Builder(trace).frames(updatedFrames).build()
    );
  }

  int totalFramesToKeep() {
    return headFrameCount + tailFrameCount;
  }

  List<Frame> truncateFrames(List<Frame> frames) {
    ArrayList<Frame> updatedFrames = new ArrayList<>();

    for (int j = 0; j < headFrameCount; ++j) {
      updatedFrames.add(frames.get(j));
    }

    int size = frames.size();
    for (int j = size - tailFrameCount; j < size; ++j) {
      updatedFrames.add(frames.get(j));
    }
    return updatedFrames;
  }

  private <T extends BodyContent> TruncationResult<Payload> mapResult(Payload payload,
                                                                      TruncationResult<T> result) {
    if (!result.wasTruncated) {
      return TruncationResult.none();
    }

    Payload newPayload = new Payload.Builder(payload).data(
        new Data.Builder(payload.getData()).body(
            new Body.Builder(payload.getData().getBody())
                .bodyContent(result.value).build()
        ).build()
    ).build();

    return TruncationResult.truncated(newPayload);
  }
}
