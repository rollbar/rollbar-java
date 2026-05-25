package com.rollbar.agent;

import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.notifier.telemetry.RollbarTelemetryEventTracker;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;

import java.util.List;

public final class AgentTelemetryStore {

  private static volatile TelemetryEventTracker INSTANCE =
      new RollbarTelemetryEventTracker(System::currentTimeMillis, 100);

  private AgentTelemetryStore() {}

  public static TelemetryEventTracker getInstance() {
    return INSTANCE;
  }

  public static List<TelemetryEvent> getAll() {
    return INSTANCE.getAll();
  }

  public static void resetForTesting() {
    INSTANCE = new RollbarTelemetryEventTracker(System::currentTimeMillis, 100);
  }
}
