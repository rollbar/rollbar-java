# Data scrubbing

Every occurrence the notifier builds — anything reported through `log`, `debug`, `info`,
`warning`, `error` or `critical`, including uncaught exceptions — is passed through a built-in
scrubber before it is sent. It runs **after** any `Transformer` you configure, so a transformer
cannot be used to opt out of it.

This applies to all three notifiers, since they share the same configuration and send path:

| Module | Covered |
| --- | --- |
| `rollbar-java` | yes |
| `rollbar-reactive-streams` | yes |
| `rollbar-android` | yes |

The exception is `Rollbar.sendJsonPayload(String)`, which hands an already-serialized payload
straight to the sender and skips transformers, filters and scrubbing alike. Nothing on this page
applies to it; scrub that JSON yourself before passing it in.

## What is redacted without any configuration

- **Request headers**, matched case-insensitively against a built-in deny-list:
  `Authorization`, `Cookie`, `Set-Cookie`, `X-Api-Key`, `X-Auth-Token`, `X-Access-Token`,
  `X-Secret`, `Proxy-Authorization`, `WWW-Authenticate`. The value becomes `***`.
- **URLs**, which have their userinfo, query string and fragment stripped. This covers
  `request.url` and the URLs recorded by `Rollbar.recordNetworkEventFor(...)`, so
  `https://user:pass@example.com/orders?token=secret` is reported as
  `https://example.com/orders`.

## Redacting your own keys

`redactedKeys` takes a list of **case-insensitive regexes**. A key is redacted when the regex is
found anywhere in it, so `"password"` also matches `user_password`.

```java
Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN)
    .redactedKeys(Arrays.asList("password", "secret", "ssn"))
    .build();
```

They are matched against the keys of: request headers, routing parameters (`request.params`),
GET and POST parameters, `request.metadata`, the raw `request.query_string`, custom data, and
`Frame.locals` — including the copies carried by `body.threads` when JVMTI locals capture is
enabled. Matching values are replaced with `***`.

Nested data is walked recursively through maps, collections and arrays, up to 8 levels of
nesting, and the surrounding shape is preserved. Given `redactedKeys(["password"])`:

```java
rollbar.error(exception, Collections.singletonMap(
    "users", Arrays.asList(Collections.singletonMap("password", "hunter2"))));
// sent as: {"users": [{"password": "***"}]}
```

When a key itself matches, its whole value is replaced rather than descended into.

## Customizing URL sanitization

Supply a `StringUrlSanitizer` to change or disable the URL handling:

```java
Config config = ConfigBuilder.withAccessToken(ACCESS_TOKEN)
    .urlSanitizer(url -> url)  // keep URLs verbatim
    .build();
```

If you use the OkHttp interceptor, share the same sanitizer so both paths redact identically:

```java
OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(RollbarOkHttpInterceptor.withSharedUrlSanitizer(
        recorder, config.urlSanitizer()))
    .build();
```

See the [rollbar-okhttp README](rollbar-okhttp/README.md) for the interceptor's own sanitizer
options.

## Migrating

This is a behaviour change: no configuration is required to get the redaction above, and it
cannot be disabled from a `Transformer`. If you are upgrading, expect that

- values matching the header deny-list or your `redactedKeys` now arrive as `***`;
- `request.url` and network telemetry URLs no longer carry credentials, query strings or
  fragments. If you rely on query parameters for grouping or search, configure a
  `urlSanitizer` that preserves them.
