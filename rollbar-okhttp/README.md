# Rollbar OkHttp Integration

This module provides an [OkHttp Interceptor](https://square.github.io/okhttp/features/interceptors/) that automatically captures network telemetry for the Rollbar Java SDK.

It records:

- **Network telemetry events** for HTTP responses with status code `>= 400` (client and server errors).
- **Error events** for connection failures, timeouts, and other I/O exceptions.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.rollbar:rollbar-okhttp:<version>")
    implementation("com.squareup.okhttp3:okhttp:<okhttp-version>")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.rollbar:rollbar-okhttp:<version>'
    implementation 'com.squareup.okhttp3:okhttp:<okhttp-version>'
}
```

## Usage

### 1. Implement `NetworkTelemetryRecorder`

```java
NetworkTelemetryRecorder recorder = new NetworkTelemetryRecorder() {
    @Override
    public void recordNetworkEvent(Level level, String method, String url, String statusCode) {
        rollbar.recordNetworkEventFor(level, method, url, statusCode);
    }

    @Override
    public void recordErrorEvent(Exception exception) {
        rollbar.log(exception);
    }
};
```

### 2. Add the interceptor to your OkHttpClient

```java
OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(new RollbarOkHttpInterceptor(recorder))
    .build();
```

### 3. Make requests as usual

```java
Request request = new Request.Builder()
    .url("https://api.example.com/data")
    .build();

Response response = client.newCall(request).execute();
```

The interceptor will automatically record telemetry events to Rollbar without interfering with the request/response flow.

## Behavior

| Scenario                          | Action                                                  |
|-----------------------------------|---------------------------------------------------------|
| Recorder is `null`                | No telemetry or log is recorded                         |
| Response status `< 400`           | No telemetry recorded, response returned normally       |
| Response status `>= 400`          | Records a network telemetry event with `Level.CRITICAL` |
| Connection failure / timeout      | Records an error event, then rethrows the `IOException` |
