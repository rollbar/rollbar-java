package com.rollbar.agent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;

/**
 * Runs in a JVM started with {@code -javaagent:} and <em>no Rollbar SDK on the classpath</em>,
 * launched by {@link AgentClassLoaderIsolationTest}. It must not reference any SDK type, junit, or
 * anything else outside the JDK and the agent jar itself.
 *
 * <p>Prints {@code probe-ok events=<n>} on success; anything else (or a non-zero exit) is the
 * failure the test reports.
 */
public final class PremainProbe {

  private PremainProbe() {}

  public static void main(String[] args) throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    } // closed here, so the request below is refused

    HttpURLConnection connection =
        (HttpURLConnection) new URL("http://127.0.0.1:" + closedPort + "/x").openConnection();
    connection.setConnectTimeout(2000);
    connection.setReadTimeout(2000);
    try {
      connection.getResponseCode();
    } catch (IOException expected) {
      // expected: nothing is listening on the port
    }
    connection.disconnect();

    System.out.println("probe-ok events=" + AgentTelemetryStore.getAll().size());
  }
}
