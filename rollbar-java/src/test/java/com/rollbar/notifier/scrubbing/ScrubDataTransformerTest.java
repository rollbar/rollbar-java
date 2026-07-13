package com.rollbar.notifier.scrubbing;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Trace;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
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
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("X-My-Secret"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .headers(headers("X-My-Secret", "sensitive", "Content-Type", "text/plain"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getHeaders().get("X-My-Secret"));
    assertEquals("text/plain", result.getRequest().getHeaders().get("Content-Type"));
  }

  @Test
  public void userKeyRedactsGetParams() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("apiToken"), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .get(getParams("apiToken", "secret-value"))
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE,
        result.getRequest().getGet().get("apiToken").get(0));
  }

  @Test
  public void userKeyRedactsPostParams() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("password"), NO_OP_SANITIZER);
    Map<String, Object> post = objectMap("password", "hunter2", "username", "alice");
    Request req = new Request.Builder().post(post).build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getRequest().getPost().get("password"));
    assertEquals("alice", result.getRequest().getPost().get("username"));
  }

  @Test
  public void userKeyRedactsCustomMap() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("mySecret"), NO_OP_SANITIZER);
    Map<String, Object> custom = objectMap("mySecret", "hidden", "other", "visible");
    Data result = t.transform(dataWithCustom(custom));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getCustom().get("mySecret"));
    assertEquals("visible", result.getCustom().get("other"));
  }

  @Test
  public void userKeyRegexMatchesMultipleKeys() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList(".*[Pp]assword.*"), NO_OP_SANITIZER);
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
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), strip);
    Request req = new Request.Builder()
        .url("https://example.com/api?secret=xyz")
        .build();
    Data result = t.transform(dataWithRequest(req));
    assertEquals("https://example.com/clean", result.getRequest().getUrl());
  }

  // --- queryString ---

  @Test
  public void queryStringValueRedacted() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("token"), NO_OP_SANITIZER);
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
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    String qs = "page=1&sort=asc";
    Request req = new Request.Builder().queryString(qs).build();
    Data result = t.transform(dataWithRequest(req));
    assertSame(qs, result.getRequest().getQueryString());
  }

  // --- Frame.locals ---

  @Test
  public void frameLocalsMatchingKeysScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("password"), NO_OP_SANITIZER);
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
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    Data data = new Data.Builder().environment("test").build();
    assertSame(data, t.transform(data));
  }

  @Test
  public void nullHeadersMapNoNpe() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder().url("https://example.com").build(); // headers null
    Data result = t.transform(dataWithRequest(req));
    assertNotNull(result);
  }

  @Test
  public void nullCustomMapNoNpe() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("secret"), NO_OP_SANITIZER);
    Data data = new Data.Builder().environment("test").build(); // custom null
    Data result = t.transform(data);
    assertNotNull(result);
  }

  @Test
  public void noMatchReturnsSameDataInstance() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
    Request req = new Request.Builder()
        .url("https://example.com")
        .headers(headers("Content-Type", "application/json"))
        .build();
    Data data = dataWithRequest(req);
    assertSame(data, t.transform(data));
  }

  @Test
  public void emptyRedactedKeysOnlyScrubsDefaultHeaders() {
    ScrubDataTransformer t = new ScrubDataTransformer(Collections.<String>emptyList(), NO_OP_SANITIZER);
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
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder().queryString("token&page=1").build();
    Data result = t.transform(dataWithRequest(req));
    String qs = result.getRequest().getQueryString();
    assertTrue(qs.contains("token=" + ScrubDataTransformer.SCRUBBED_VALUE));
    assertTrue(qs.contains("page=1"));
  }

  @Test
  public void valueLessQueryParamNoMatchPassedThrough() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("token"), NO_OP_SANITIZER);
    Request req = new Request.Builder().queryString("debug&page=1").build();
    Data result = t.transform(dataWithRequest(req));
    assertSame(req.getQueryString(), result.getRequest().getQueryString());
  }

  // --- nested map scrubbing (B4 fix) ---

  @Test
  public void nestedCustomMapKeysScrubbed() {
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("password"), NO_OP_SANITIZER);
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
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("token"), NO_OP_SANITIZER);
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
    ScrubDataTransformer t = new ScrubDataTransformer(Arrays.asList("credentials"), NO_OP_SANITIZER);
    Map<String, Object> inner = objectMap("password", "hunter2");
    Map<String, Object> custom = new HashMap<>();
    custom.put("credentials", inner);
    Data result = t.transform(dataWithCustom(custom));
    assertEquals(ScrubDataTransformer.SCRUBBED_VALUE, result.getCustom().get("credentials"));
  }
}
