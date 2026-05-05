# Rollbar OkHttp Integration

This module provides an [OkHttp Interceptor](https://square.github.io/okhttp/features/interceptors/) that automatically captures network telemetry for the Rollbar Java SDK.

It records:

- **Network telemetry events** for HTTP responses with status code `>= 400` (client and server errors).
- **Error events** for connection failures, timeouts, and other I/O exceptions.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.rollbar:rollbar-java:<version>")
    implementation("com.rollbar:rollbar-okhttp:<version>")
    implementation("com.squareup.okhttp3:okhttp:<okhttp-version>")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.rollbar:rollbar-java:<version>'
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
        // url has userinfo, query parameters, and fragment stripped by default
        // (see Security section below)
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

## Security

URLs can carry sensitive data in several components. To prevent accidental leakage to Rollbar, the interceptor **strips userinfo (basic-auth credentials), query parameters, and the fragment by default** before passing the URL to `NetworkTelemetryRecorder`.

For example, a request to `https://user:secret@api.example.com/charge?token=sk_live_secret#section` will be recorded as `https://api.example.com/charge`.

If your URLs do not contain sensitive query parameters and you need them for debugging, you can opt in to the full URL by supplying a custom sanitizer:

```java
OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(new RollbarOkHttpInterceptor(recorder, HttpUrl::toString))
    .build();
```

When using a custom sanitizer, you are responsible for ensuring that sensitive query parameters are removed before the URL reaches Rollbar.
