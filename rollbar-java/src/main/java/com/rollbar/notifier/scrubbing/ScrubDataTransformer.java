package com.rollbar.notifier.scrubbing;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.BodyContent;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import com.rollbar.notifier.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * <p>Additional keys (matched as case-insensitive regex against header names, query/POST
 * parameter keys, custom data keys, and {@code Frame.locals} keys) can be configured via
 * {@code ConfigBuilder.redactedKeys(List)}.
 */
public final class ScrubDataTransformer implements Transformer {

  public static final String SCRUBBED_VALUE = "***";

  // O(1) set lookup; avoids Matcher allocation on every header key.
  private static final Set<String> DEFAULT_HEADERS = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          "authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token",
          "x-access-token", "x-secret", "proxy-authorization", "www-authenticate"
      ))
  );

  // Recursion cap for nested Map values in custom data and Frame.locals.
  private static final int MAX_SCRUB_DEPTH = 8;

  private final List<Pattern> fieldPatterns;
  private final StringUrlSanitizer urlSanitizer;

  public ScrubDataTransformer(List<String> redactedKeys, StringUrlSanitizer urlSanitizer) {
    this.urlSanitizer = urlSanitizer != null ? urlSanitizer : DefaultUrlSanitizer.INSTANCE;
    if (redactedKeys == null || redactedKeys.isEmpty()) {
      this.fieldPatterns = Collections.emptyList();
    } else {
      List<Pattern> patterns = new ArrayList<>(redactedKeys.size());
      for (String key : redactedKeys) {
        patterns.add(Pattern.compile(key, Pattern.CASE_INSENSITIVE));
      }
      this.fieldPatterns = Collections.unmodifiableList(patterns);
    }
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
    Map<String, Object> scrubbedCustom = scrubObjectMap(originalCustom, fieldPatterns, 0);
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
    Map<String, List<String>> originalGet = req.getGet();
    Map<String, Object> originalPost = req.getPost();
    String originalQueryString = req.getQueryString();

    String scrubbedUrl = originalUrl != null ? urlSanitizer.sanitize(originalUrl) : null;
    Map<String, String> scrubbedHeaders = scrubStringMap(originalHeaders);
    Map<String, List<String>> scrubbedGet = scrubMultiMap(originalGet, fieldPatterns);
    Map<String, Object> scrubbedPost = scrubObjectMap(originalPost, fieldPatterns, 0);
    String scrubbedQueryString = scrubQueryString(originalQueryString, fieldPatterns);

    boolean changed = !equal(originalUrl, scrubbedUrl)
        || scrubbedHeaders != originalHeaders
        || scrubbedGet != originalGet
        || scrubbedPost != originalPost
        || !equal(originalQueryString, scrubbedQueryString);

    if (!changed) {
      return req;
    }

    return new Request.Builder(req)
        .url(scrubbedUrl)
        .headers(scrubbedHeaders)
        .get(scrubbedGet)
        .post(scrubbedPost)
        .queryString(scrubbedQueryString)
        .build();
  }

  private Body scrubBody(Body body) {
    if (body == null || fieldPatterns.isEmpty()) {
      return body;
    }
    BodyContent content = body.getContents();
    if (content instanceof Trace) {
      Trace scrubbed = scrubTrace((Trace) content);
      if (scrubbed != content) {
        return new Body.Builder(body).bodyContent(scrubbed).build();
      }
    } else if (content instanceof TraceChain) {
      TraceChain chain = (TraceChain) content;
      List<Trace> traces = chain.getTraces();
      if (traces != null) {
        List<Trace> scrubbed = new ArrayList<>(traces.size());
        boolean anyChanged = false;
        for (Trace trace : traces) {
          Trace st = scrubTrace(trace);
          scrubbed.add(st);
          if (st != trace) {
            anyChanged = true;
          }
        }
        if (anyChanged) {
          return new Body.Builder(body)
              .bodyContent(new TraceChain.Builder(chain).traces(scrubbed).build())
              .build();
        }
      }
    }
    return body;
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
    Map<String, Object> scrubbedLocals = scrubObjectMap(locals, fieldPatterns, 0);
    if (scrubbedLocals == locals) {
      return frame;
    }
    return new Frame.Builder(frame).locals(scrubbedLocals).build();
  }

  private Map<String, String> scrubStringMap(Map<String, String> map) {
    if (map == null) {
      return null;
    }
    Map<String, String> result = null;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = entry.getKey();
      if (matchesDefaultHeader(key) || matchesAny(key, fieldPatterns)) {
        if (result == null) {
          result = new HashMap<>(map);
        }
        result.put(key, SCRUBBED_VALUE);
      }
    }
    return result != null ? result : map;
  }

  private Map<String, Object> scrubObjectMap(Map<String, Object> map, List<Pattern> patterns,
      int depth) {
    if (map == null || patterns.isEmpty()) {
      return map;
    }
    Map<String, Object> result = null;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (matchesAny(key, patterns)) {
        if (result == null) {
          result = new HashMap<>(map);
        }
        result.put(key, SCRUBBED_VALUE);
      } else if (value instanceof Map && depth < MAX_SCRUB_DEPTH) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) value;
        Map<String, Object> scrubbedNested = scrubObjectMap(nested, patterns, depth + 1);
        if (scrubbedNested != nested) {
          if (result == null) {
            result = new HashMap<>(map);
          }
          result.put(key, scrubbedNested);
        }
      }
    }
    return result != null ? result : map;
  }

  private Map<String, List<String>> scrubMultiMap(Map<String, List<String>> map,
      List<Pattern> patterns) {
    if (map == null || patterns.isEmpty()) {
      return map;
    }
    Map<String, List<String>> result = null;
    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
      if (matchesAny(entry.getKey(), patterns)) {
        if (result == null) {
          result = new HashMap<>(map);
        }
        result.put(entry.getKey(), Collections.singletonList(SCRUBBED_VALUE));
      }
    }
    return result != null ? result : map;
  }

  private String scrubQueryString(String queryString, List<Pattern> patterns) {
    if (queryString == null || queryString.isEmpty() || patterns.isEmpty()) {
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
      if (matchesAny(key, patterns)) {
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

  private static boolean matchesDefaultHeader(String key) {
    return DEFAULT_HEADERS.contains(key.toLowerCase(Locale.ROOT));
  }

  private static boolean matchesAny(String key, List<Pattern> patterns) {
    for (Pattern p : patterns) {
      if (p.matcher(key).find()) {
        return true;
      }
    }
    return false;
  }

  private static boolean equal(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }
}
