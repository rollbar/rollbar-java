package com.rollbar.agent;

import com.rollbar.agent.instrumentation.ApacheHttpClient4Instrumentation;
import com.rollbar.agent.instrumentation.ApacheHttpClient5Instrumentation;
import com.rollbar.agent.instrumentation.HttpUrlConnectionInstrumentation;
import com.rollbar.agent.instrumentation.JavaHttpClientInstrumentation;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.api.payload.data.TelemetryEvent;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * Java agent entry point. Attach with {@code -javaagent:/path/to/rollbar-java-agent.jar}.
 *
 * <p>Wire into your Rollbar configuration with:
 * <pre>
 *   Rollbar.init(withAccessToken("...")
 *       .telemetryEventTracker(RollbarAgent.getTelemetryTracker())
 *       .build());
 * </pre>
 */
public class RollbarAgent {

  private RollbarAgent() {}

  public static void premain(String args, Instrumentation inst) {
    installInstrumentation(inst);
  }

  public static void agentmain(String args, Instrumentation inst) {
    installInstrumentation(inst);
  }

  private static void installInstrumentation(Instrumentation inst) {
    // Override ByteBuddy's default which ignores all java.* and javax.* classes,
    // so we can instrument JDK HTTP clients (HttpURLConnection, HttpClient).
    // We still ignore ByteBuddy's own classes to avoid instrumentation loops.
    AgentBuilder builder = new AgentBuilder.Default()
        .ignore(ElementMatchers.nameStartsWith("net.bytebuddy.")
            .or(ElementMatchers.nameStartsWith("com.rollbar.agent.shaded.")))
        .with(new ErrorReportingListener())
        .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE);

    HttpUrlConnectionInstrumentation.install(builder, inst);
    JavaHttpClientInstrumentation.installIfAvailable(builder, inst);
    ApacheHttpClient4Instrumentation.installIfAvailable(builder, inst);
    ApacheHttpClient5Instrumentation.installIfAvailable(builder, inst);
  }

  /**
   * Reports transformation failures to {@code System.err}.
   *
   * <p>ByteBuddy's default listener discards them, which turns the most likely whole-agent failure
   * into a silent one: ByteBuddy can only parse class files up to the JDK version it was built
   * against, and the classes this agent instruments ({@code HttpURLConnection},
   * {@code HttpClient}) always carry the running JDK's class file version. Run on a JDK newer than
   * the bundled ByteBuddy and every transformation fails to parse — the agent installs cleanly,
   * records nothing, and the first symptom is missing telemetry. Bumping ByteBuddy fixes today's
   * JDKs; this listener is what makes tomorrow's diagnosable.
   *
   * <p>Output is capped: a version mismatch fails for every instrumented type, and an agent must
   * not flood a process's stderr.
   */
  static final class ErrorReportingListener extends AgentBuilder.Listener.Adapter {

    static final int MAX_REPORTS = 10;

    private final AtomicInteger reported = new AtomicInteger();

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module,
        boolean loaded, Throwable throwable) {
      int count = reported.incrementAndGet();
      if (count > MAX_REPORTS) {
        return;
      }
      System.err.println("[rollbar-java-agent] failed to instrument " + typeName + ": "
          + throwable);
      if (count == MAX_REPORTS) {
        System.err.println("[rollbar-java-agent] further instrumentation errors suppressed; "
            + "if these mention an unsupported class file version, this agent's ByteBuddy is "
            + "older than the JDK it is running on");
      }
    }
  }

  public static TelemetryEventTracker getTelemetryTracker() {
    return DelegatingTracker.INSTANCE;
  }

  private static final class DelegatingTracker implements TelemetryEventTracker {

    static final DelegatingTracker INSTANCE = new DelegatingTracker();

    private DelegatingTracker() {}

    @Override
    public List<TelemetryEvent> getAll() {
      return AgentTelemetryStore.getInstance().getAll();
    }

    @Override
    public void recordLogEventFor(Level level, Source source, String message) {
      AgentTelemetryStore.getInstance().recordLogEventFor(level, source, message);
    }

    @Override
    public void recordManualEventFor(Level level, Source source, String message) {
      AgentTelemetryStore.getInstance().recordManualEventFor(level, source, message);
    }

    @Override
    public void recordNavigationEventFor(Level level, Source source, String from, String to) {
      AgentTelemetryStore.getInstance().recordNavigationEventFor(level, source, from, to);
    }

    @Override
    public void recordNetworkEventFor(
        Level level, Source source, String method, String url, String statusCode) {
      AgentTelemetryStore.getInstance().recordNetworkEventFor(
          level, source, method, url, statusCode);
    }
  }
}
