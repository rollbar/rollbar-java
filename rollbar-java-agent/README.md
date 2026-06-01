# Rollbar Java Agent

A zero-code-change Java instrumentation agent that automatically captures HTTP network errors (4xx and 5xx responses) as Rollbar telemetry events.

It works by attaching to the JVM at startup via `-javaagent:` and using ByteBuddy to intercept HTTP calls across all major clients — no library dependencies or code changes are needed in your application.

## Instrumented HTTP clients

| Client | Condition |
|--------|-----------|
| `java.net.HttpURLConnection` | Always (JDK built-in) |
| `java.net.http.HttpClient` — `send()` and `sendAsync()` | Java 11+ only |
| Apache HttpClient 4.x (`org.apache.http`) | If present on classpath |
| Apache HttpClient 5.x (`org.apache.hc.client5`) | If present on classpath |

Only 4xx and 5xx responses are recorded. Successful requests (< 400) produce no telemetry.

> **Apache HC4/HC5 note:** `execute(request, responseHandler)` overloads route through a separate internal call chain and are not instrumented. Use the standard `execute(request)` / `execute(request, context)` overloads to get telemetry.

## Requirements

- Java 11 or higher
- `rollbar-java` on the application classpath (for `Rollbar.init(...)`)

## Installation

### 1. Build the agent JAR

```bash
./gradlew :rollbar-java-agent:shadowJar
```

The fat JAR (with ByteBuddy bundled and relocated) is written to:

```
rollbar-java-agent/build/libs/rollbar-java-agent-<version>.jar
```

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

### 3. Also add the JAR to your classpath

The agent JAR must also be available on the regular application classpath so that your code can call `RollbarAgent.getTelemetryTracker()`:

**Gradle:**
```kotlin
dependencies {
    implementation(files("/path/to/rollbar-java-agent-<version>.jar"))
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.rollbar</groupId>
    <artifactId>rollbar-java-agent</artifactId>
    <version>${rollbar.version}</version>
</dependency>
```

### 4. Wire into your Rollbar configuration

```java
import com.rollbar.agent.RollbarAgent;
import com.rollbar.notifier.Rollbar;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

Rollbar rollbar = Rollbar.init(
    withAccessToken("your-access-token")
        .environment("production")
        .telemetryEventTracker(RollbarAgent.getTelemetryTracker())
        .build()
);
```

That's it. All HTTP calls your application makes from that point on will automatically produce telemetry events in the Rollbar error report for any 4xx or 5xx response.

## Behavior

| Scenario | Action |
|----------|--------|
| Response status `< 400` | No telemetry recorded |
| Response status `>= 400` | Records a network telemetry event with `Level.CRITICAL` |
| Connection failure / I/O error | Records an error telemetry event |
| No Rollbar config wired | Events accumulate in the agent store (capacity 100); nothing is sent |

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
   import com.rollbar.agent.RollbarAgent;
   import com.rollbar.notifier.Rollbar;

   import java.net.HttpURLConnection;
   import java.net.URL;

   import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

   public class SmokeTest {
       public static void main(String[] args) throws Exception {
           Rollbar rollbar = Rollbar.init(
               withAccessToken("your-access-token")
                   .environment("test")
                   .telemetryEventTracker(RollbarAgent.getTelemetryTracker())
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
        -cp "rollbar-java-agent/build/libs/rollbar-java-agent-<version>.jar:your-app.jar" \
        SmokeTest
   ```

4. Check your Rollbar dashboard — the error report for "smoke test error" should show a **Network** telemetry event for the 404 in the telemetry timeline.
