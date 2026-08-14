package com.rollbar.notifier.scrubbing;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.BodyContent;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Group;
import com.rollbar.api.payload.data.body.RollbarThread;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import com.rollbar.api.scrubbing.DefaultUrlSanitizer;
import com.rollbar.api.scrubbing.StringUrlSanitizer;
import com.rollbar.notifier.transformer.Transformer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Built-in {@link Transformer} that scrubs sensitive values from payloads before they are sent
 * to Rollbar. Applied automatically after any user-provided transformer.
 *
 * <p>By default the following request headers are redacted:
 * {@code Authorization}, {@code Cookie}, {@code Set-Cookie}, {@code X-Api-Key},
 * {@code X-Auth-Token}, {@code X-Access-Token}, {@code X-Secret},
 * {@code Proxy-Authorization}, {@code WWW-Authenticate}.
 *
 * <p>Field keys are matched against {@link #DEFAULT_REDACTED_KEYS} plus any key configured via
 * {@code ConfigBuilder.redactedKeys(List)}; the two lists are additive. They are matched as
 * case-insensitive regexes against header names, routing parameter keys
 * ({@code Request.params}), query and POST parameter keys, request metadata keys
 * ({@code Request.metadata}), custom data keys, and {@code Frame.locals} keys.
 * {@code Frame.locals} are scrubbed both in the top-level body content and in the trace chains
 * carried by {@code Body.rollbarThreads}. The built-in defaults can be turned off with
 * {@code ConfigBuilder.useDefaultRedactedKeys(false)}, leaving only the configured keys.
 *
 * <p>Nested data is walked recursively: maps reachable through other maps, through
 * {@link Collection}s and through object arrays are all scrubbed, up to 8 levels of nesting. The
 * surrounding shape is preserved, so a list stays a list and an array stays an array.
 *
 * <p>The built-in header deny-list above applies to {@code Request.headers} only; every other
 * slot matches on the field keys alone.
 */
public final class ScrubDataTransformer implements Transformer {

  public static final String SCRUBBED_VALUE = "***";

  /**
   * Field keys redacted out of the box, matched as case-insensitive regexes anywhere in the key
   * unless anchored. Substring matching makes these cover the usual variants: {@code password}
   * also matches {@code user_password} and {@code passwordConfirmation}, {@code token} also
   * matches {@code access_token}, {@code auth_token} and {@code csrfToken}, and {@code secret}
   * also matches {@code client_secret}.
   *
   * <p>{@code auth} is anchored so that only a key that is exactly {@code auth} matches; leaving
   * it unanchored would redact innocuous keys such as {@code author}. Its longer forms are listed
   * separately.
   *
   * <p>The api key spellings are listed one by one rather than as {@code api[-_]?key} so that the
   * whole default list is free of regex syntax, which lets {@link KeyMatcher} match it without
   * allocating.
   */
  public static final List<String> DEFAULT_REDACTED_KEYS = Collections.unmodifiableList(
      Arrays.asList(
          "password", "passwd", "secret", "token", "authorization", "authentication", "^auth$",
          "apikey", "api_key", "api-key"
      )
  );

  // O(1) set lookup; avoids Matcher allocation on every header key.
  private static final Set<String> DEFAULT_HEADERS = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          "authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token",
          "x-access-token", "x-secret", "proxy-authorization", "www-authenticate"
      ))
  );

  // Recursion cap for nested containers in custom data, request payloads and Frame.locals. Every
  // map, collection or array counts as one level; this also terminates cyclic structures.
  private static final int MAX_SCRUB_DEPTH = 8;

  private final KeyMatcher fieldKeys;
  private final StringUrlSanitizer urlSanitizer;

  /**
   * Constructor using the built-in {@link #DEFAULT_REDACTED_KEYS}.
   *
   * @param redactedKeys keys to redact on top of the defaults, matched as case-insensitive
   *     regexes. May be {@code null} or empty.
   * @param urlSanitizer sanitizer applied to the request URL. Falls back to
   *     {@link DefaultUrlSanitizer#INSTANCE} when {@code null}.
   */
  public ScrubDataTransformer(List<String> redactedKeys, StringUrlSanitizer urlSanitizer) {
    this(redactedKeys, urlSanitizer, true);
  }

  /**
   * Constructor.
   *
   * @param redactedKeys keys to redact, matched as case-insensitive regexes. May be {@code null}
   *     or empty.
   * @param urlSanitizer sanitizer applied to the request URL. Falls back to
   *     {@link DefaultUrlSanitizer#INSTANCE} when {@code null}.
   * @param useDefaultRedactedKeys whether {@link #DEFAULT_REDACTED_KEYS} are matched in addition
   *     to {@code redactedKeys}. When {@code false} only {@code redactedKeys} apply, so passing
   *     an empty list leaves the header deny-list and the URL sanitizer as the only redaction.
   */
  public ScrubDataTransformer(List<String> redactedKeys, StringUrlSanitizer urlSanitizer,
      boolean useDefaultRedactedKeys) {
    this.urlSanitizer = urlSanitizer != null ? urlSanitizer : DefaultUrlSanitizer.INSTANCE;
    this.fieldKeys = KeyMatcher.of(redactedKeys, useDefaultRedactedKeys);
  }

  @Override
  public Data transform(Data data) {
    if (data == null) {
      return null;
    }

    Request originalRequest = data.getRequest();
    Map<String, Object> originalCustom = data.getCustom();
    Body originalBody = data.getBody();

    Request scrubbedRequest = scrubRequest(originalRequest);
    Map<String, Object> scrubbedCustom = scrubObjectMap(originalCustom, fieldKeys, 0);
    Body scrubbedBody = scrubBody(originalBody);

    boolean changed = scrubbedRequest != originalRequest
        || scrubbedCustom != originalCustom
        || scrubbedBody != originalBody;

    if (!changed) {
      return data;
    }

    Data.Builder builder = new Data.Builder(data);
    if (scrubbedRequest != originalRequest) {
      builder.request(scrubbedRequest);
    }
    if (scrubbedCustom != originalCustom) {
      builder.custom(scrubbedCustom);
    }
    if (scrubbedBody != originalBody) {
      builder.body(scrubbedBody);
    }
    return builder.build();
  }

  private Request scrubRequest(Request req) {
    if (req == null) {
      return null;
    }

    String originalUrl = req.getUrl();
    Map<String, String> originalHeaders = req.getHeaders();
    Map<String, String> originalParams = req.getParams();
    Map<String, List<String>> originalGet = req.getGet();
    Map<String, Object> originalPost = req.getPost();
    Map<String, Object> originalMetadata = req.getMetadata();
    String originalQueryString = req.getQueryString();

    String scrubbedUrl = originalUrl != null ? urlSanitizer.sanitize(originalUrl) : null;
    Map<String, String> scrubbedHeaders = scrubHeaders(originalHeaders);
    Map<String, String> scrubbedParams = scrubStringMap(originalParams, fieldKeys);
    Map<String, List<String>> scrubbedGet = scrubMultiMap(originalGet, fieldKeys);
    Map<String, Object> scrubbedPost = scrubObjectMap(originalPost, fieldKeys, 0);
    Map<String, Object> scrubbedMetadata = scrubObjectMap(originalMetadata, fieldKeys, 0);
    String scrubbedQueryString = scrubQueryString(originalQueryString, fieldKeys);

    boolean changed = !equal(originalUrl, scrubbedUrl)
        || scrubbedHeaders != originalHeaders
        || scrubbedParams != originalParams
        || scrubbedGet != originalGet
        || scrubbedPost != originalPost
        || scrubbedMetadata != originalMetadata
        || !equal(originalQueryString, scrubbedQueryString);

    if (!changed) {
      return req;
    }

    return new Request.Builder(req)
        .url(scrubbedUrl)
        .headers(scrubbedHeaders)
        .params(scrubbedParams)
        .get(scrubbedGet)
        .post(scrubbedPost)
        .metadata(scrubbedMetadata)
        .queryString(scrubbedQueryString)
        .build();
  }

  private Body scrubBody(Body body) {
    if (body == null || fieldKeys.isEmpty()) {
      return body;
    }

    BodyContent originalContent = body.getContents();
    List<RollbarThread> originalThreads = body.getRollbarThreads();

    BodyContent scrubbedContent = scrubBodyContent(originalContent);
    List<RollbarThread> scrubbedThreads = scrubThreads(originalThreads);

    if (scrubbedContent == originalContent && scrubbedThreads == originalThreads) {
      return body;
    }

    return new Body.Builder(body)
        .bodyContent(scrubbedContent)
        .rollbarThreads(scrubbedThreads)
        .build();
  }

  private BodyContent scrubBodyContent(BodyContent content) {
    if (content instanceof Trace) {
      return scrubTrace((Trace) content);
    } else if (content instanceof TraceChain) {
      return scrubTraceChain((TraceChain) content);
    }
    return content;
  }

  /**
   * The initial thread carries the same frames as the top-level body content, so its locals must
   * be scrubbed too, otherwise the {@code threads} entry leaks what {@code trace} redacted.
   */
  private List<RollbarThread> scrubThreads(List<RollbarThread> threads) {
    if (threads == null || threads.isEmpty()) {
      return threads;
    }
    List<RollbarThread> scrubbed = new ArrayList<>(threads.size());
    boolean anyChanged = false;
    for (RollbarThread thread : threads) {
      RollbarThread st = scrubThread(thread);
      scrubbed.add(st);
      if (st != thread) {
        anyChanged = true;
      }
    }
    return anyChanged ? scrubbed : threads;
  }

  private RollbarThread scrubThread(RollbarThread thread) {
    if (thread == null || thread.getGroup() == null) {
      return thread;
    }
    TraceChain chain = thread.getGroup().getTraceChain();
    TraceChain scrubbedChain = scrubTraceChain(chain);
    if (scrubbedChain == chain) {
      return thread;
    }
    return new RollbarThread.Builder(thread).group(new Group(scrubbedChain)).build();
  }

  private TraceChain scrubTraceChain(TraceChain chain) {
    if (chain == null) {
      return null;
    }
    List<Trace> traces = chain.getTraces();
    if (traces == null || traces.isEmpty()) {
      return chain;
    }
    List<Trace> scrubbed = new ArrayList<>(traces.size());
    boolean anyChanged = false;
    for (Trace trace : traces) {
      Trace st = scrubTrace(trace);
      scrubbed.add(st);
      if (st != trace) {
        anyChanged = true;
      }
    }
    if (!anyChanged) {
      return chain;
    }
    return new TraceChain.Builder(chain).traces(scrubbed).build();
  }

  private Trace scrubTrace(Trace trace) {
    if (trace == null) {
      return null;
    }
    List<Frame> frames = trace.getFrames();
    if (frames == null || frames.isEmpty()) {
      return trace;
    }
    List<Frame> scrubbed = new ArrayList<>(frames.size());
    boolean anyChanged = false;
    for (Frame frame : frames) {
      Frame sf = scrubFrame(frame);
      scrubbed.add(sf);
      if (sf != frame) {
        anyChanged = true;
      }
    }
    if (!anyChanged) {
      return trace;
    }
    return new Trace.Builder(trace).frames(scrubbed).build();
  }

  private Frame scrubFrame(Frame frame) {
    if (frame == null) {
      return null;
    }
    Map<String, Object> locals = frame.getLocals();
    Map<String, Object> scrubbedLocals = scrubObjectMap(locals, fieldKeys, 0);
    if (scrubbedLocals == locals) {
      return frame;
    }
    return new Frame.Builder(frame).locals(scrubbedLocals).build();
  }

  private Map<String, String> scrubHeaders(Map<String, String> map) {
    if (map == null) {
      return null;
    }
    Map<String, String> result = null;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = entry.getKey();
      if (matchesDefaultHeader(key) || fieldKeys.matches(key)) {
        if (result == null) {
          result = new HashMap<>(map);
        }
        result.put(key, SCRUBBED_VALUE);
      }
    }
    return result != null ? result : map;
  }

  /**
   * Scrubs a flat string map against the configured keys only. The built-in header deny-list is
   * deliberately not applied here: it names HTTP headers, and a routing parameter such as
   * {@code /cookie/:id} is not one. This keeps routing params consistent with the GET/POST
   * parameter maps, which also match on {@code redactedKeys} alone.
   */
  private Map<String, String> scrubStringMap(Map<String, String> map, KeyMatcher keys) {
    if (map == null || keys.isEmpty()) {
      return map;
    }
    Map<String, String> result = null;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = entry.getKey();
      if (keys.matches(key)) {
        if (result == null) {
          result = new HashMap<>(map);
        }
        result.put(key, SCRUBBED_VALUE);
      }
    }
    return result != null ? result : map;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> scrubObjectMap(Map<String, Object> map, KeyMatcher keys,
      int depth) {
    if (map == null || keys.isEmpty()) {
      return map;
    }
    // scrubMap only ever copies keys across, so a Map<String, Object> in stays one on the way out.
    return (Map<String, Object>) scrubMap(map, keys, depth);
  }

  /**
   * Recursively scrubs a nested value. Maps are scrubbed by key; collections and object arrays are
   * traversed so that the maps they contain are scrubbed too, preserving the surrounding shape.
   * Every container counts as one level against {@code MAX_SCRUB_DEPTH}, which also terminates
   * cyclic structures. Anything else is returned untouched.
   */
  private Object scrubNested(Object value, KeyMatcher keys, int depth) {
    if (depth >= MAX_SCRUB_DEPTH) {
      return value;
    }
    if (value instanceof Map) {
      return scrubMap((Map<?, ?>) value, keys, depth + 1);
    }
    if (value instanceof Collection) {
      return scrubCollection((Collection<?>) value, keys, depth + 1);
    }
    if (value instanceof Object[]) {
      return scrubArray((Object[]) value, keys, depth + 1);
    }
    return value;
  }

  private Object scrubMap(Map<?, ?> map, KeyMatcher keys, int depth) {
    Map<Object, Object> result = null;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      // A non-String key cannot match a redactedKeys pattern, but its value is still traversed.
      boolean keyMatches = key instanceof String && keys.matches((String) key);
      Object scrubbed = keyMatches ? SCRUBBED_VALUE : scrubNested(value, keys, depth);
      if (keyMatches || scrubbed != value) {
        if (result == null) {
          result = new LinkedHashMap<>(map);
        }
        result.put(key, scrubbed);
      }
    }
    return result != null ? result : map;
  }

  private Object scrubCollection(Collection<?> collection, KeyMatcher keys, int depth) {
    if (collection.isEmpty()) {
      return collection;
    }
    List<Object> scrubbed = new ArrayList<>(collection.size());
    boolean changed = false;
    for (Object element : collection) {
      Object scrubbedElement = scrubNested(element, keys, depth);
      scrubbed.add(scrubbedElement);
      if (scrubbedElement != element) {
        changed = true;
      }
    }
    if (!changed) {
      return collection;
    }
    // Sets keep set semantics; any other Collection serializes as a JSON array either way.
    // A SortedSet is deliberately downgraded to insertion order: a rebuilt map is not Comparable.
    return collection instanceof Set ? new LinkedHashSet<>(scrubbed) : scrubbed;
  }

  private Object scrubArray(Object[] array, KeyMatcher keys, int depth) {
    Object[] result = null;
    for (int i = 0; i < array.length; i++) {
      Object scrubbedElement = scrubNested(array[i], keys, depth);
      if (scrubbedElement != array[i]) {
        if (result == null) {
          // Object[] rather than array.clone(): a rebuilt value may not fit the original component
          // type (e.g. a HashMap[] receiving a LinkedHashMap), which would throw
          // ArrayStoreException.
          result = new Object[array.length];
          System.arraycopy(array, 0, result, 0, array.length);
        }
        result[i] = scrubbedElement;
      }
    }
    return result != null ? result : array;
  }

  private Map<String, List<String>> scrubMultiMap(Map<String, List<String>> map,
      KeyMatcher keys) {
    if (map == null || keys.isEmpty()) {
      return map;
    }
    Map<String, List<String>> result = null;
    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
      if (keys.matches(entry.getKey())) {
        if (result == null) {
          result = new HashMap<>(map);
        }
        result.put(entry.getKey(), Collections.singletonList(SCRUBBED_VALUE));
      }
    }
    return result != null ? result : map;
  }

  private String scrubQueryString(String queryString, KeyMatcher keys) {
    if (queryString == null || queryString.isEmpty() || keys.isEmpty()) {
      return queryString;
    }
    String[] pairs = queryString.split("&", -1);
    boolean changed = false;
    String[] output = new String[pairs.length];
    for (int i = 0; i < pairs.length; i++) {
      String pair = pairs[i];
      int eq = pair.indexOf('=');
      // A value-less param (e.g. "?token") is treated as key-only and scrubbed the same way.
      String key = eq >= 0 ? pair.substring(0, eq) : pair;
      if (keys.matches(key) || keys.matches(decodeParamName(key))) {
        output[i] = key + "=" + SCRUBBED_VALUE;
        changed = true;
      } else {
        output[i] = pair;
      }
    }
    if (!changed) {
      return queryString;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < output.length; i++) {
      if (i > 0) {
        sb.append('&');
      }
      sb.append(output[i]);
    }
    return sb.toString();
  }

  /**
   * Percent-decodes a query parameter name. Malformed escapes (e.g. {@code %zz}) are left as-is
   * rather than failing the transform: the raw form is still matched by the caller.
   */
  private static String decodeParamName(String key) {
    if (key.indexOf('%') < 0 && key.indexOf('+') < 0) {
      return key;
    }
    try {
      return URLDecoder.decode(key, "UTF-8");
    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
      return key;
    }
  }

  private static boolean matchesDefaultHeader(String key) {
    return DEFAULT_HEADERS.contains(key.toLowerCase(Locale.ROOT));
  }

  private static boolean equal(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }

  /**
   * Matches a key against the redacted key list. Keys are documented as case-insensitive regexes
   * and keep those semantics, but running a {@link Pattern} over every key of every payload
   * allocates a {@code Matcher} per key per pattern, which on a ~100 key payload is the bulk of
   * what this transformer costs. In practice almost every key is a plain name - the whole
   * built-in list is - so plain names are matched with one lowercase pass and a substring search,
   * and only keys that actually carry regex syntax reach {@link Pattern}.
   *
   * <p>{@code String.toLowerCase} returns the receiver when there is nothing to fold, so a key
   * that is already lower case costs no allocation at all.
   */
  private static final class KeyMatcher {

    // Special outside a character class, or the opening of one. '-' is deliberately absent: it
    // only carries meaning inside a class, so "x-api-key" stays a literal.
    private static final String METACHARACTERS = "\\.[]{}()*+?^$|";

    private final String[] contains;
    private final String[] exact;
    private final Pattern[] patterns;

    private KeyMatcher(List<String> contains, List<String> exact, List<Pattern> patterns) {
      this.contains = contains.toArray(new String[0]);
      this.exact = exact.toArray(new String[0]);
      this.patterns = patterns.toArray(new Pattern[0]);
    }

    static KeyMatcher of(List<String> redactedKeys, boolean useDefaultRedactedKeys) {
      List<String> keys = new ArrayList<>();
      if (useDefaultRedactedKeys) {
        keys.addAll(DEFAULT_REDACTED_KEYS);
      }
      if (redactedKeys != null) {
        keys.addAll(redactedKeys);
      }

      List<String> contains = new ArrayList<>();
      List<String> exact = new ArrayList<>();
      List<Pattern> patterns = new ArrayList<>();
      for (String key : keys) {
        String anchored = anchoredLiteral(key);
        if (anchored != null) {
          exact.add(anchored.toLowerCase(Locale.ROOT));
        } else if (isLiteral(key)) {
          contains.add(key.toLowerCase(Locale.ROOT));
        } else {
          // Compiled up front, so an invalid regex still fails when the notifier is configured
          // rather than when the first payload is scrubbed.
          patterns.add(Pattern.compile(key, Pattern.CASE_INSENSITIVE));
        }
      }
      return new KeyMatcher(contains, exact, patterns);
    }

    boolean isEmpty() {
      return contains.length == 0 && exact.length == 0 && patterns.length == 0;
    }

    boolean matches(String key) {
      if (contains.length > 0 || exact.length > 0) {
        // CASE_INSENSITIVE folds ASCII only while toLowerCase folds the whole of Unicode, so an
        // exotic key can match here where the equivalent regex would not. That direction only
        // ever redacts more, which is the safe way to differ.
        String lower = key.toLowerCase(Locale.ROOT);
        for (String needle : contains) {
          if (lower.contains(needle)) {
            return true;
          }
        }
        for (String name : exact) {
          if (lower.equals(name)) {
            return true;
          }
        }
      }
      for (Pattern pattern : patterns) {
        if (pattern.matcher(key).find()) {
          return true;
        }
      }
      return false;
    }

    /**
     * The literal inside an anchored key such as {@code ^auth$}, or null if it is not one.
     */
    private static String anchoredLiteral(String key) {
      if (key.length() <= 2 || key.charAt(0) != '^' || key.charAt(key.length() - 1) != '$') {
        return null;
      }
      String inner = key.substring(1, key.length() - 1);
      return isLiteral(inner) ? inner : null;
    }

    private static boolean isLiteral(String key) {
      for (int i = 0; i < key.length(); i++) {
        if (METACHARACTERS.indexOf(key.charAt(i)) >= 0) {
          return false;
        }
      }
      return true;
    }
  }
}
