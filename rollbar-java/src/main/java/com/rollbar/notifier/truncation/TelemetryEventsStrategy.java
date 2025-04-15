package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.api.payload.data.body.Body;

import java.util.ArrayList;
import java.util.List;

public class TelemetryEventsStrategy implements TruncationStrategy {
  private static final int MAX_EVENTS = 10;

  @Override
  public TruncationResult<Payload> truncate(Payload payload) {
    if (payload == null || payload.getData() == null || payload.getData().getBody() == null) {
      return TruncationResult.none();
    }

    Body body = payload.getData().getBody();
    List<TelemetryEvent> telemetryEvents = body.getTelemetryEvents();
    if (telemetryEvents == null || telemetryEvents.size() <= MAX_EVENTS) {
      return TruncationResult.none();
    }

    ArrayList<TelemetryEvent> truncatedTelemetryEvents = new ArrayList<>();
    for (int i = 0; i < MAX_EVENTS; i++) {
      truncatedTelemetryEvents.add(telemetryEvents.get(i));
    }

    Payload newPayload = new Payload.Builder(payload).data(
        new Data.Builder(payload.getData()).body(
            new Body
                .Builder(payload.getData().getBody())
                .telemetryEvents(truncatedTelemetryEvents)
                .build()
        ).build()
    ).build();

    return TruncationResult.truncated(newPayload);
  }
}
