package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Group;
import com.rollbar.api.payload.data.body.RollbarThread;
import com.rollbar.api.payload.data.body.TraceChain;

import java.util.ArrayList;
import java.util.List;

public class RollbarThreadStrategy implements TruncationStrategy {
  private static final FramesStrategy FRAMES_STRATEGY = new FramesStrategy();

  @Override
  public TruncationResult<Payload> truncate(Payload payload) {
    if (payload == null || payload.getData() == null || payload.getData().getBody() == null) {
      return TruncationResult.none();
    }

    Body body = payload.getData().getBody();
    List<RollbarThread> rollbarThreads = body.getRollbarThreads();
    if (rollbarThreads == null) {
      return TruncationResult.none();
    }

    TruncationResult<List<RollbarThread>> truncationResult = truncateRollbarThreads(rollbarThreads);
    if (!truncationResult.wasTruncated) {
      return TruncationResult.none();
    }

    Payload newPayload = new Payload.Builder(payload).data(
        new Data.Builder(payload.getData()).body(
            new Body.Builder(payload.getData().getBody())
                .rollbarThreads(truncationResult.value).build()
        ).build()
    ).build();

    return TruncationResult.truncated(newPayload);
  }

  private TruncationResult<List<RollbarThread>> truncateRollbarThreads(
      List<RollbarThread> rollbarThreads
  ) {
    boolean truncated = false;
    ArrayList<RollbarThread> truncatedThreads = new ArrayList<>();
    for (RollbarThread rollbarThread: rollbarThreads) {
      TraceChain traceChain = rollbarThread.getGroup().getTraceChain();

      TruncationResult<TraceChain> result = FRAMES_STRATEGY.truncateTraceChain(traceChain);
      if (result.wasTruncated) {
        truncated = true;
        traceChain = result.value;
      }

      RollbarThread truncatedThread = new RollbarThread
          .Builder(rollbarThread)
          .group(new Group(traceChain)).build();
      truncatedThreads.add(truncatedThread);
    }

    if (truncated) {
      return TruncationResult.truncated(truncatedThreads);
    } else {
      return TruncationResult.none();
    }
  }
}
