package com.rollbar.agent;

import com.rollbar.agent.instrumentation.ApacheHttpClient4Instrumentation;
import com.rollbar.agent.instrumentation.ApacheHttpClient5Instrumentation;
import com.rollbar.agent.instrumentation.HttpURLConnectionInstrumentation;
import com.rollbar.agent.instrumentation.JavaHttpClientInstrumentation;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

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
        .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE);

    HttpURLConnectionInstrumentation.install(builder, inst);
    JavaHttpClientInstrumentation.installIfAvailable(builder, inst);
    ApacheHttpClient4Instrumentation.installIfAvailable(builder, inst);
    ApacheHttpClient5Instrumentation.installIfAvailable(builder, inst);
  }

  public static TelemetryEventTracker getTelemetryTracker() {
    return AgentTelemetryStore.getInstance();
  }
}
