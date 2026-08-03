package com.rollbar.notifier.scrubbing;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Group;
import com.rollbar.api.payload.data.body.RollbarThread;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import com.rollbar.api.scrubbing.StringUrlSanitizer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ScrubDataTransformerTest {

  private static final StringUrlSanitizer NO_OP_SANITIZER = url -> url;

  // --- helpers ---

  private static Data dataWithRequest(Request request) {
    return new Data.Builder()
        .environment("test")
        .request(request)
        .build();
  }

  private static Data dataWithCustom(Map<String, Object> custom) {
    return new Data.Builder()
        .environment("test")
        .custom(custom)
        .build();
  }

  private static Map<String, String> headers(String... kvPairs) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < kvPairs.length; i += 2) {
      map.put(kvPairs[i], kvPairs[i + 1]);
    }
    return map;
  }

  private static Map<String, List<String>> getParams(String key, String value) {
    Map<String, List<String>> map = new HashMap<>();
    map.put(key, Collections.singletonList(value));
    return map;
  }

  private static Map<String, Object> objectMap(String... kvPairs) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < kvPairs.length; i += 2) {
      map.put(kvPairs[i], kvPairs[i + 1]);
    }
    return map;
  }

  // --- default header deny-list ---

  @Test
  public void authorizationHeaderRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .headers(headers("Authorization", "Bearer secret-token", "Content-Type", "application/json"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("Authorization"));
    assertEquals("application/json", result.getRequest().getHeaders().get("Content-Type"));
  }

  @Test
  public void cookieHeaderRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .headers(headers("Cookie", "session=abc123"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("Cookie"));
  }

  @Test
  public void setCookieHeaderRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .headers(headers("Set-Cookie", "session=abc123; HttpOnly"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("Set-Cookie"));
  }

  @Test
  public void xApiKeyHeaderRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .headers(headers("X-Api-Key", "key-12345"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("X-Api-Key"));
  }

  @Test
  public void caseInsensitiveHeaderMatching() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .headers(headers("AUTHORIZATION", "Basic xyz", "authorization", "Bearer abc"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("AUTHORIZATION"));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("authorization"));
  }

  // --- user redactedKeys ---

  @Test
  public void userKeyRedactsHeaders() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("X-My-Secret"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .headers(headers("X-My-Secret", "sensitive", "Content-Type", "text/plain"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("X-My-Secret"));
    assertEquals("text/plain", result.getRequest().getHeaders().get("Content-Type"));
  }

  @Test
  public void userKeyRedactsGetParams() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("apiToken"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .get(getParams("apiToken", "secret-value"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE,
        result.getRequest().getGet().get("apiToken").get(0));
  }

  @Test
  public void userKeyRedactsPostParams() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> post = objectMap("password", "hunter2", "username", "alice");
    Request req = new Request.Builder().post(post).build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getPost().get("password"));
    assertEquals("alice", result.getRequest().getPost().get("username"));
  }

  @Test
  public void userKeyRedactsCustomMap() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("mySecret"), NO_OP_SANITIZER);
    Map<String, Object> custom = objectMap("mySecret", "hidden", "other", "visible");
    Data result = t.transform(dataWithCustom(custom));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getCustom().get("mySecret"));
    assertEquals("visible", result.getCustom().get("other"));
  }

  @Test
  public void userKeyRedactsRoutingParams() {
    // e.g. a /reset/:token route populating Request.params.
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .params(headers("token", "reset-token-abc", "userId", "42"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getParams().get("token"));
    assertEquals("42", result.getRequest().getParams().get("userId"));
  }

  @Test
  public void routingParamsNotMatchedByHeaderDenyList() {
    // The built-in deny-list names HTTP headers; a routing param called "cookie" is not one.
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .params(headers("cookie", "chocolate-chip"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals("chocolate-chip", result.getRequest().getParams().get("cookie"));
  }

  @Test
  public void userKeyRedactsMetadata() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("apiKey"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .metadata(objectMap("apiKey", "key-12345", "region", "us-east-1"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getMetadata().get("apiKey"));
    assertEquals("us-east-1", result.getRequest().getMetadata().get("region"));
  }

  @Test
  public void nestedMetadataKeysScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> inner = objectMap("password", "hunter2", "user", "alice");
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("auth", inner);
    Request req = new Request.Builder().metadata(metadata).build();
    Data result = t.transform(dataWithRequest(req));
    @SuppressWarnings("unchecked")
    Map<String, Object> scrubbed = (Map<String, Object>) result.getRequest().getMetadata().get("auth");
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, scrubbed.get("password"));
    assertEquals("alice", scrubbed.get("user"));
  }

  @Test
  public void nullParamsAndMetadataNoNpe() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Request req = new Request.Builder().url("https://example.com").build();
    Data result = t.transform(dataWithRequest(req));
    assertNull(result.getRequest().getParams());
    assertNull(result.getRequest().getMetadata());
  }

  @Test
  public void userKeyRegexMatchesMultipleKeys() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList(".*[Pp]assword.*"), NO_OP_SANITIZER);
    Map<String, Object> custom = objectMap(
        "passwordHash", "xxx",
        "oldPassword", "yyy",
        "username", "alice"
    );
    Data result = t.transform(dataWithCustom(custom));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getCustom().get("passwordHash"));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getCustom().get("oldPassword"));
    assertEquals("alice", result.getCustom().get("username"));
  }

  // --- URL sanitizer ---

  @Test
  public void urlSanitizedViaProvidedSanitizer() {
    StringUrlSanitizer strip = url -> "https://example.com/clean";
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), strip);
    Request req = new Request.Builder()
        .url("https://example.com/api?secret=xyz")
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals("https://example.com/clean", result.getRequest().getUrl());
  }

  // --- queryString ---

  @Test
  public void queryStringValueRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .queryString("token=secret&page=1")
        .build();
    Data result = t.transform(dataWithRequest(req));
    String qs = result.getRequest().getQueryString();
    assertTrue(qs.contains("token=" + ScrubDataTransformer.SCRUBBED_VALUE));
    assertTrue(qs.contains("page=1"));
  }

  @Test
  public void queryStringUnchangedWhenNoMatch() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), NO_OP_SANITIZER);
    String qs = "page=1&sort=asc";
    Request req = new Request.Builder().queryString(qs).build();
    Data result = t.transform(dataWithRequest(req));
    assertSame(qs, result.getRequest().getQueryString());
  }

  @Test
  public void percentEncodedQueryParamNameRedacted() {
    // getQueryString() is raw, so "pass%77ord" is semantically the "password" param.
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .queryString("pass%77ord=hunter2&page=1")
        .build();
    Data result = t.transform(dataWithRequest(req));
    String qs = result.getRequest().getQueryString();
    assertFalse(qs.contains("hunter2"));
    // The original encoding of the key is preserved; only the value is replaced.
    assertTrue(qs.contains("pass%77ord=" + ScrubDataTransformer.SCRUBBED_VALUE));
    assertTrue(qs.contains("page=1"));
  }

  @Test
  public void fullyPercentEncodedQueryParamNameRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .queryString("%70%61%73%73%77%6F%72%64=hunter2")
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertFalse(result.getRequest().getQueryString().contains("hunter2"));
  }

  @Test
  public void plusEncodedQueryParamNameRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("user password"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .queryString("user+password=hunter2")
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertFalse(result.getRequest().getQueryString().contains("hunter2"));
  }

  @Test
  public void malformedEscapeInQueryParamNameFallsBackToRawMatch() {
    // %zz is not a valid escape; decoding fails and the raw name is matched instead.
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .queryString("token%zz=secret&page=1")
        .build();
    Data result = t.transform(dataWithRequest(req));
    String qs = result.getRequest().getQueryString();
    assertFalse(qs.contains("secret"));
    assertTrue(qs.contains("page=1"));
  }

  @Test
  public void malformedEscapeInNonMatchingQueryParamPassedThrough() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    String qs = "page%zz=1";
    Request req = new Request.Builder().queryString(qs).build();
    Data result = t.transform(dataWithRequest(req));
    assertSame(qs, result.getRequest().getQueryString());
  }

  @Test
  public void encodedValueLessQueryParamScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder().queryString("t%6Fken").build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals("t%6Fken=" + ScrubDataTransformer.SCRUBBED_VALUE,
        result.getRequest().getQueryString());
  }

  // --- Frame.locals ---

  @Test
  public void frameLocalsMatchingKeysScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> locals = objectMap("password", "secret", "userId", "42");
    Frame frame = new Frame.Builder().locals(locals).build();
    Trace trace = new Trace.Builder().frames(Collections.singletonList(frame)).build();
    Body body = new Body.Builder().bodyContent(trace).build();
    Data data = new Data.Builder().environment("test").body(body).build();

    Data result = t.transform(data);

    List<Frame> frames = ((Trace) result.getBody().getContents()).getFrames();
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, frames.get(0).getLocals().get("password"));
    assertEquals("42", frames.get(0).getLocals().get("userId"));
  }

  // --- null-safety ---

  @Test
  public void nullRequestReturnsDataUnchanged() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), NO_OP_SANITIZER);
    Data data = new Data.Builder().environment("test").build();
    assertSame(data, t.transform(data));
  }

  @Test
  public void nullHeadersMapNoNpe() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder().url("https://example.com").build(); // headers null
    Data result = t.transform(dataWithRequest(req));
    assertNotNull(result);
  }

  @Test
  public void nullCustomMapNoNpe() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("secret"), NO_OP_SANITIZER);
    Data data = new Data.Builder().environment("test").build(); // custom null
    Data result = t.transform(data);
    assertNotNull(result);
  }

  @Test
  public void noMatchReturnsSameDataInstance() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .url("https://example.com")
        .headers(headers("Content-Type", "application/json"))
        .build();
    Data data = dataWithRequest(req);
    assertSame(data, t.transform(data));
  }

  @Test
  public void emptyRedactedKeysOnlyScrubsDefaultHeaders() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.emptyList(), NO_OP_SANITIZER);
    Map<String, Object> custom = objectMap("myApiKey", "visible");
    Map<String, String> hdrs = headers("Authorization", "Bearer xyz", "Content-Type", "text/html");
    Request req = new Request.Builder().headers(hdrs).build();
    Data data = new Data.Builder().environment("test").request(req).custom(custom).build();

    Data result = t.transform(data);
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("Authorization"));
    assertEquals("text/html", result.getRequest().getHeaders().get("Content-Type"));
    // custom key not in default deny-list → not scrubbed
    assertEquals("visible", result.getCustom().get("myApiKey"));
  }

  @Test
  public void nullDataReturnsNull() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    assertNull(t.transform(null));
  }

  // --- value-less query params (B1 fix) ---

  @Test
  public void valueLessQueryParamScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder().queryString("token&page=1").build();
    Data result = t.transform(dataWithRequest(req));
    String qs = result.getRequest().getQueryString();
    assertTrue(qs.contains("token=" + ScrubDataTransformer.SCRUBBED_VALUE));
    assertTrue(qs.contains("page=1"));
  }

  @Test
  public void valueLessQueryParamNoMatchPassedThrough() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder().queryString("debug&page=1").build();
    Data result = t.transform(dataWithRequest(req));
    assertSame(req.getQueryString(), result.getRequest().getQueryString());
  }

  // --- nested map scrubbing (B4 fix) ---

  @Test
  public void nestedCustomMapKeysScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> inner = objectMap("password", "hunter2", "user", "alice");
    Map<String, Object> custom = new HashMap<>();
    custom.put("auth", inner);
    custom.put("visible", "yes");
    Data result = t.transform(dataWithCustom(custom));
    @SuppressWarnings("unchecked")
    Map<String, Object> scrubbed = (Map<String, Object>) result.getCustom().get("auth");
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, scrubbed.get("password"));
    assertEquals("alice", scrubbed.get("user"));
    assertEquals("yes", result.getCustom().get("visible"));
  }

  @Test
  public void nestedFrameLocalsKeysScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Map<String, Object> inner = objectMap("token", "secret-token", "count", "5");
    Map<String, Object> locals = new HashMap<>();
    locals.put("credentials", inner);
    locals.put("userId", "42");
    Frame frame = new Frame.Builder().locals(locals).build();
    Trace trace = new Trace.Builder().frames(Collections.singletonList(frame)).build();
    Body body = new Body.Builder().bodyContent(trace).build();
    Data data = new Data.Builder().environment("test").body(body).build();

    Data result = t.transform(data);
    List<Frame> frames = ((Trace) result.getBody().getContents()).getFrames();
    @SuppressWarnings("unchecked")
    Map<String, Object> scrubbedInner =
        (Map<String, Object>) frames.get(0).getLocals().get("credentials");
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, scrubbedInner.get("token"));
    assertEquals("5", scrubbedInner.get("count"));
    assertEquals("42", frames.get(0).getLocals().get("userId"));
  }

  @Test
  public void nestedMapParentKeyMatchScrubsEntireValue() {
    // When the top-level key itself matches, the whole nested map is replaced, not recursed.
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("credentials"), NO_OP_SANITIZER);
    Map<String, Object> inner = objectMap("password", "hunter2");
    Map<String, Object> custom = new HashMap<>();
    custom.put("credentials", inner);
    Data result = t.transform(dataWithCustom(custom));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getCustom().get("credentials"));
  }

  @Test
  public void threadFrameLocalsScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Body body = new Body.Builder()
        .bodyContent(traceWithLocals(objectMap("password", "hunter2", "userId", "42")))
        .rollbarThreads(Collections.singletonList(
            threadWithLocals(objectMap("password", "hunter2", "userId", "42"))))
        .build();
    Data data = new Data.Builder().environment("test").body(body).build();

    Data result = t.transform(data);

    Map<String, Object> locals = threadLocals(result.getBody(), 0);
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, locals.get("password"));
    assertEquals("42", locals.get("userId"));
    // The top-level trace is still scrubbed.
    List<Frame> frames = ((Trace) result.getBody().getContents()).getFrames();
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, frames.get(0).getLocals().get("password"));
  }

  @Test
  public void threadFrameLocalsScrubbedWhenBodyContentHasNoMatch() {
    // Regression: the threads entry must be scrubbed even when the body content needs no change.
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Body body = new Body.Builder()
        .bodyContent(traceWithLocals(objectMap("userId", "42")))
        .rollbarThreads(Collections.singletonList(
            threadWithLocals(objectMap("token", "secret-token"))))
        .build();
    Data data = new Data.Builder().environment("test").body(body).build();

    Data result = t.transform(data);

    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE,
        threadLocals(result.getBody(), 0).get("token"));
  }

  @Test
  public void threadsWithNoMatchReturnSameBodyInstance() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Body body = new Body.Builder()
        .bodyContent(traceWithLocals(objectMap("userId", "42")))
        .rollbarThreads(Collections.singletonList(threadWithLocals(objectMap("userId", "42"))))
        .build();
    Data data = new Data.Builder().environment("test").body(body).build();

    assertSame(data, t.transform(data));
  }

  @Test
  public void nullThreadsNoNpe() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Body body = new Body.Builder()
        .bodyContent(traceWithLocals(objectMap("password", "hunter2")))
        .build(); // rollbarThreads null
    Data data = new Data.Builder().environment("test").body(body).build();

    Data result = t.transform(data);

    List<Frame> frames = ((Trace) result.getBody().getContents()).getFrames();
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, frames.get(0).getLocals().get("password"));
    assertNull(result.getBody().getRollbarThreads());
  }

  // --- collections and arrays (P1 fix) ---

  @Test
  public void listOfMapsInCustomScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> custom = new HashMap<>();
    custom.put("users", Collections.singletonList(objectMap("password", "hunter2", "name", "alice")));
    Data result = t.transform(dataWithCustom(custom));

    List<?> users = (List<?>) result.getCustom().get("users");
    Map<?, ?> user = (Map<?, ?>) users.get(0);
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, user.get("password"));
    assertEquals("alice", user.get("name"));
  }

  @Test
  public void arrayOfMapsInCustomScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> custom = new HashMap<>();
    custom.put("users", new Object[] {objectMap("password", "hunter2", "name", "alice")});
    Data result = t.transform(dataWithCustom(custom));

    Object[] users = (Object[]) result.getCustom().get("users");
    Map<?, ?> user = (Map<?, ?>) users[0];
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, user.get("password"));
    assertEquals("alice", user.get("name"));
  }

  @Test
  public void nestedListScrubbedInRequestPost() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> post = new HashMap<>();
    post.put("users", Collections.singletonList(objectMap("password", "hunter2")));
    Request req = new Request.Builder().post(post).build();

    Data result = t.transform(dataWithRequest(req));

    List<?> users = (List<?>) result.getRequest().getPost().get("users");
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, ((Map<?, ?>) users.get(0)).get("password"));
  }

  @Test
  public void nestedListScrubbedInRequestMetadata() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("users", Collections.singletonList(objectMap("password", "hunter2")));
    Request req = new Request.Builder().metadata(metadata).build();

    Data result = t.transform(dataWithRequest(req));

    List<?> users = (List<?>) result.getRequest().getMetadata().get("users");
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, ((Map<?, ?>) users.get(0)).get("password"));
  }

  @Test
  public void nestedArrayScrubbedInFrameLocals() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("token"), NO_OP_SANITIZER);
    Map<String, Object> locals = new HashMap<>();
    locals.put("sessions", new Object[] {objectMap("token", "secret-token")});
    Body body = new Body.Builder().bodyContent(traceWithLocals(locals)).build();
    Data data = new Data.Builder().environment("test").body(body).build();

    Data result = t.transform(data);

    List<Frame> frames = ((Trace) result.getBody().getContents()).getFrames();
    Object[] sessions = (Object[]) frames.get(0).getLocals().get("sessions");
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, ((Map<?, ?>) sessions[0]).get("token"));
  }

  @Test
  public void listOrderAndSizePreservedWhenScrubbing() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> custom = new HashMap<>();
    custom.put("rows", Arrays.asList(objectMap("password", "hunter2"), "plain", objectMap("name", "bob")));
    Data result = t.transform(dataWithCustom(custom));

    Object scrubbed = result.getCustom().get("rows");
    assertTrue(scrubbed instanceof List);
    List<?> rows = (List<?>) scrubbed;
    assertEquals(3, rows.size());
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, ((Map<?, ?>) rows.get(0)).get("password"));
    assertEquals("plain", rows.get(1));
    assertEquals("bob", ((Map<?, ?>) rows.get(2)).get("name"));
  }

  @Test
  public void setShapePreservedWhenScrubbing() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Set<Object> rows = new LinkedHashSet<>();
    rows.add(objectMap("password", "hunter2"));
    rows.add("plain");
    Map<String, Object> custom = new HashMap<>();
    custom.put("rows", rows);
    Data result = t.transform(dataWithCustom(custom));

    Object scrubbed = result.getCustom().get("rows");
    assertTrue(scrubbed instanceof Set);
    Set<?> scrubbedRows = (Set<?>) scrubbed;
    assertEquals(2, scrubbedRows.size());
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE,
        ((Map<?, ?>) scrubbedRows.iterator().next()).get("password"));
  }

  @Test
  public void typedArrayScrubbedWithoutArrayStoreException() {
    // The rebuilt map may not fit the original component type, so the array is widened on copy.
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    HashMap<?, ?>[] rows = new HashMap<?, ?>[] {(HashMap<?, ?>) objectMap("password", "hunter2")};
    Map<String, Object> custom = new HashMap<>();
    custom.put("rows", rows);

    Data result = t.transform(dataWithCustom(custom));

    Object[] scrubbed = (Object[]) result.getCustom().get("rows");
    assertEquals(1, scrubbed.length);
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, ((Map<?, ?>) scrubbed[0]).get("password"));
  }

  @Test
  public void collectionWithNoMatchReturnsSameInstances() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    List<Object> users = Collections.singletonList(objectMap("name", "alice"));
    Map<String, Object> custom = new HashMap<>();
    custom.put("users", users);
    Data data = dataWithCustom(custom);

    Data result = t.transform(data);

    assertSame(data, result);
    assertSame(users, result.getCustom().get("users"));
  }

  @Test
  public void collectionNestingWithinDepthCapScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> custom = new HashMap<>();
    custom.put("root", nestInLists(objectMap("password", "hunter2"), 7));

    Data result = t.transform(dataWithCustom(custom));

    Map<?, ?> leaf = (Map<?, ?>) unwrapLists(result.getCustom().get("root"), 7);
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, leaf.get("password"));
  }

  @Test
  public void collectionNestingBeyondDepthCapNotScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<String, Object> custom = new HashMap<>();
    custom.put("root", nestInLists(objectMap("password", "hunter2"), 8));

    Data result = t.transform(dataWithCustom(custom));

    Map<?, ?> leaf = (Map<?, ?>) unwrapLists(result.getCustom().get("root"), 8);
    assertEquals("hunter2", leaf.get("password"));
  }

  @Test
  public void selfReferencingCollectionTerminates() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    List<Object> cycle = new ArrayList<>();
    cycle.add(objectMap("password", "hunter2"));
    cycle.add(cycle);
    Map<String, Object> custom = new HashMap<>();
    custom.put("cycle", cycle);

    Data result = t.transform(dataWithCustom(custom));

    List<?> scrubbed = (List<?>) result.getCustom().get("cycle");
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE,
        ((Map<?, ?>) scrubbed.get(0)).get("password"));
  }

  @Test
  public void nonStringMapKeysInsideCollectionDoNotThrow() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.singletonList("password"), NO_OP_SANITIZER);
    Map<Object, Object> byId = new HashMap<>();
    byId.put(1, objectMap("password", "hunter2"));
    Map<String, Object> custom = new HashMap<>();
    custom.put("rows", Collections.singletonList(byId));

    Data result = t.transform(dataWithCustom(custom));

    Map<?, ?> scrubbedById = (Map<?, ?>) ((List<?>) result.getCustom().get("rows")).get(0);
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE,
        ((Map<?, ?>) scrubbedById.get(1)).get("password"));
  }

  private static Object nestInLists(Object leaf, int levels) {
    Object current = leaf;
    for (int i = 0; i < levels; i++) {
      current = new ArrayList<>(Collections.singletonList(current));
    }
    return current;
  }

  private static Object unwrapLists(Object value, int levels) {
    Object current = value;
    for (int i = 0; i < levels; i++) {
      current = ((List<?>) current).get(0);
    }
    return current;
  }

  private static Trace traceWithLocals(Map<String, Object> locals) {
    Frame frame = new Frame.Builder().locals(locals).build();
    return new Trace.Builder().frames(Collections.singletonList(frame)).build();
  }

  private static RollbarThread threadWithLocals(Map<String, Object> locals) {
    TraceChain chain = new TraceChain.Builder()
        .traces(Collections.singletonList(traceWithLocals(locals)))
        .build();
    return new RollbarThread("main", "1", "5", "RUNNABLE", new Group(chain));
  }

  private static Map<String, Object> threadLocals(Body body, int threadIndex) {
    return body.getRollbarThreads().get(threadIndex)
        .getGroup().getTraceChain().getTraces().get(0)
        .getFrames().get(0).getLocals();
  }
}
