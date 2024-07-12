package com.rollbar.notifier.telemetry;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.TelemetryEvent;

import java.util.List;

public interface TelemetryEventTracker {
  List<TelemetryEvent> dump();
  void recordLogEventFor(Level level, String source, String message);
  void recordManualEventFor(Level level, String source, String message);
  void recordNavigationEventFor(Level level, String source, String from, String to);
  void recordNetworkEventFor(Level level, String source, String method, String url, String statusCode);
}
