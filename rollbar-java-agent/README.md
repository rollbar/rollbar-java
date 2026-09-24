# Rollbar Java Agent

A Java instrumentation agent that automatically captures HTTP network errors (4xx and 5xx responses) as Rollbar telemetry events, with no changes at your HTTP call sites.

It works by attaching to the JVM at startup via `-javaagent:` and using ByteBuddy to intercept HTTP calls across all major clients. Your request code stays exactly as it is, and you add no HTTP-related library dependencies. Setup is a one-time wiring step in your Rollbar configuration — see [What "no code changes" means here](#what-no-code-changes-means-here).

## Instrumented HTTP clients

| Client | Condition |
|--------|-----------|
| `java.net.HttpURLConnection` | Always (JDK built-in) |
| `java.net.http.HttpClient` — `send()` and `sendAsync()` | Java 11+ only |
| Apache HttpClient 4.x (`org.apache.http`) | If present on classpath |
| Apache HttpClient 5.x (`org.apache.hc.client5`) | If present on classpath |

Only 4xx and 5xx responses are recorded, along with requests that fail before a response arrives (connection refused, DNS failure, timeout). Successful requests (< 400) produce no telemetry.

**Apache HC4/HC5:** every `execute(...)` overload is covered — the request-only forms, the target-host forms (`execute(HttpHost, request)`), and the response-handler forms. The agent instruments the protected `doExecute(HttpHost, request, context)` method that all of them converge on, rather than any individual `execute()` overload, so no dispatch path is missed. Requests issued through a target-host overload carry only a path, so the agent rejoins the host from the `HttpHost` argument to record a complete URL.

### HttpURLConnection entry points

`HttpURLConnection` is captured through three entry points, so a failed request is recorded regardless of how your code consumes the response:

| Entry point | Why it is covered |
|-------------|-------------------|
| `getResponseCode()` | The caller checks the status code explicitly. |
| `getInputStream()` | The caller reads the body directly and only ever sees the `IOException` that a 4xx/5xx throws. |
| `getErrorStream()` | The caller inspects the error stream after `connect()`, or after catching the `IOException` from `getInputStream()`. |

Exactly one event is recorded per connection, even when your code hits several of these entry points (for example `getInputStream()` throwing and then `getErrorStream()` being read) — the agent deduplicates on the connection instance.

## Requirements

- Java 11 or higher **to run** the agent
- Java 17 or higher **to build** it from source — the shadow plugin that packages the fat JAR
  requires a Java 17+ JVM, so on an older JDK the module is excluded from the build entirely and
  `:rollbar-java-agent` tasks fail as unknown. The JAR it produces still targets Java 11.
- `rollbar-java` 2.3.0-beta.1 or newer on the application classpath — it supplies
  `AgentTelemetryEventTracker`, the class that reads the agent's events (step 3)

The agent bundles only ByteBuddy, under a relocated package name, and depends on nothing else —
not even the Rollbar SDK.

### Why the agent carries no Rollbar classes

`-javaagent:` appends the agent jar to the JVM's **system** class path, and most applications do
not keep their dependencies there: a Spring Boot fat jar loads them from `BOOT-INF/lib`, a WAR from
`WEB-INF/lib`, both through a child classloader. Parent delegation only looks upward, so any SDK
type named from agent code would resolve against the system classloader and not be found.

In a method signature of the agent's `Premain-Class` that is fatal *before your application
starts*: the JVM calls `getDeclaredMethods()` on it to locate `premain`, which loads every type in
every declared signature, and the resulting `NoClassDefFoundError` aborts startup with
`FATAL ERROR in native method: processing of -javaagent failed`.

So the split runs the other way. The agent holds its events as plain string maps and never names
an SDK type; `AgentTelemetryEventTracker`, which ships in `rollbar-java` and therefore loads in
your application's classloader, reaches *up* to the system classloader to read them and turns them
into `TelemetryEvent`s. Reaching up always works; reaching down never does.

## Installation

### What "no code changes" means here

All three steps below are **required**. Step 3 touches your application once, at setup:

- **What you never change:** your HTTP call sites. Every request through `HttpURLConnection`,
  `java.net.http.HttpClient`, or Apache HC 4.x/5.x is instrumented as written — no wrappers, no
  interceptors, no per-call bookkeeping, and nothing to remember when you add the next HTTP call.
- **What you change once:** the agent JAR goes on your JVM's command line (step 2), and your
  `Rollbar.init(...)` passes an `AgentTelemetryEventTracker` to the config builder (step 3).

That wiring cannot be made automatic today. `ConfigBuilder.build()` installs its default
`RollbarTelemetryEventTracker` whenever `telemetryEventTracker(...)` was not called, and the SDK
exposes no global registry or `ServiceLoader` hook that an agent could claim instead — so the
tracker has to be handed to the builder by the application. Skipping step 3 is silent: the agent
still records events, but into a buffer nothing ever reads (see [Behavior](#behavior)).

### 1. Build the agent JAR

```bash
./gradlew :rollbar-java-agent:shadowJar
```

The fat JAR (with ByteBuddy bundled and relocated) is written to:

```
rollbar-java-agent/build/libs/rollbar-java-agent-<version>.jar
```

This fat JAR is the module's only artifact — the thin `jar` task is disabled, and the shaded JAR is what Gradle consumers and the published Maven artifact resolve to.

### 2. Add the agent JVM flag

Add `-javaagent:` to your JVM startup arguments, pointing at the JAR built above:

```
-javaagent:/path/to/rollbar-java-agent-<version>.jar
```

**Gradle:**
```kotlin
jvmArgs("-javaagent:/path/to/rollbar-java-agent-<version>.jar")
```

**Maven Surefire / Failsafe:**
```xml
<argLine>-javaagent:/path/to/rollbar-java-agent-<version>.jar</argLine>
```

**Docker / environment variable:**
```bash
JAVA_TOOL_OPTIONS="-javaagent:/path/to/rollbar-java-agent-<version>.jar"
```

### 3. Wire into your Rollbar configuration (required)

```java
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.telemetry.AgentTelemetryEventTracker;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

Rollbar rollbar = Rollbar.init(
    withAccessToken("your-access-token")
        .environment("production")
        .telemetryEventTracker(new AgentTelemetryEventTracker())
        .build()
);
```

`AgentTelemetryEventTracker` comes from `rollbar-java`, which your application already depends on,
so the agent JAR itself is **not** a compile dependency — it only needs to be on the `-javaagent:`
flag. The tracker also records the events your application reports itself, exactly as the default
`RollbarTelemetryEventTracker` does, and merges both streams in timestamp order.

Without the agent attached the tracker just works as the default one, and says so once in the log —
useful when the same build runs with and without the agent.

That's the last application change you make. From here on, every HTTP call — including ones you add later — automatically produces a telemetry event in the Rollbar error report for any 4xx or 5xx response, with no further code changes.

## Behavior

| Scenario | Action |
|----------|--------|
| Response status `< 400` | No telemetry recorded |
| Response status `>= 400` | Records a network telemetry event with `Level.CRITICAL` |
| Connection failure / I/O error (connection refused, DNS failure, timeout) | Records a `Network error: <message>` telemetry event with `Level.CRITICAL` |
| The same request seen through several entry points | Deduplicated — one event per request |
| Installation step 3 not done | **Misconfiguration.** Events accumulate in the agent's buffer (capacity 100, oldest dropped) and are never sent — nothing reads them into your `Rollbar` instance. Silent apart from the missing telemetry. |

The agent never throws into your application: every advice body swallows all errors, so a failure inside the instrumentation cannot break an HTTP call.

## Security

URLs can carry sensitive data in query parameters or basic-auth credentials. The agent **strips userinfo, query parameters, and the URL fragment** before recording.

For example, a request to:
```
https://user:secret@api.example.com/charge?token=sk_live_abc#section
```
is recorded as:
```
https://api.example.com/charge
```

## Internal API

`AgentTelemetryStore.getAll()` is the contract between the agent and `rollbar-java`: it returns the
buffered events as `List<Map<String, String>>`, each map carrying `type`, `level`, `source` and
`timestamp_ms` alongside the event body. `AgentTelemetryEventTracker` calls it reflectively, so its
signature cannot change without changing both sides.

Two methods exist for tests only. Do not call them in production code.

- `AgentTelemetryStore.resetForTesting()` — drops every buffered event and restores the default clock.
- `NetworkEventBridge.resetRecordedForTesting()` — clears the deduplication state, so events from a previous test do not suppress recording in the next one.

## Testing

### Automated tests

```bash
./gradlew :rollbar-java-agent:test
```

This runs the full test suite (WireMock-backed integration tests for each instrumented client).

### Manual smoke test

1. Build the agent JAR:
   ```bash
   ./gradlew :rollbar-java-agent:shadowJar
   ```

2. Write a small program that triggers a 4xx or 5xx:
   ```java
   import com.rollbar.notifier.Rollbar;
   import com.rollbar.notifier.telemetry.AgentTelemetryEventTracker;

   import java.net.HttpURLConnection;
   import java.net.URL;

   import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

   public class SmokeTest {
       public static void main(String[] args) throws Exception {
           Rollbar rollbar = Rollbar.init(
               withAccessToken("your-access-token")
                   .environment("test")
                   .telemetryEventTracker(new AgentTelemetryEventTracker())
                   .build()
           );

           // Trigger a 404 — captured as a telemetry event on the next error report
           HttpURLConnection conn = (HttpURLConnection) new URL("https://httpstat.us/404").openConnection();
           int code = conn.getResponseCode();
           conn.disconnect();

           System.out.println("Response: " + code);

           // Send an error to Rollbar — the 404 telemetry event will appear alongside it
           rollbar.error(new RuntimeException("smoke test error"));
       }
   }
   ```

3. Run with the agent:
   ```bash
   java -javaagent:rollbar-java-agent/build/libs/rollbar-java-agent-<version>.jar \
        -cp "your-app.jar" \
        SmokeTest
   ```

4. Check your Rollbar dashboard — the error report for "smoke test error" should show a **Network** telemetry event for the 404 in the telemetry timeline.
