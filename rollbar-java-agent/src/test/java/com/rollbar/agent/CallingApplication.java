package com.rollbar.agent;

/**
 * Stands in for application code that makes an instrumented HTTP call.
 *
 * <p>{@link AgentClassLoaderIsolationTest} loads a copy of this class into a classloader of its
 * own, so that the frame this class contributes to the stack belongs to that "application" while
 * the thread's context classloader says something else entirely.
 */
public final class CallingApplication {

  private CallingApplication() {}

  public static void makeCall(String url) {
    AgentTelemetryStore.recordNetworkEvent("GET", url, "500");
  }
}
