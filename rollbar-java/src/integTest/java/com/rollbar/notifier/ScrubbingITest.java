package com.rollbar.notifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Request;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.scrubbing.ScrubDataTransformer;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.transformer.Transformer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * End-to-end coverage of the built-in scrubber: it must run after any user transformer, follow
 * reconfiguration, reach data nested inside collections, and keep network telemetry URLs clean.
 * Assertions are made against the JSON WireMock actually received, so the whole serialization
 * path is exercised.
 */
public class ScrubbingITest {

  private static final String ACCESS_TOKEN = UUID.randomUUID().toString();

  private static final String SCRUBBED = ScrubDataTransformer.SCRUBBED_VALUE;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  private Sender sender;

  private ConfigBuilder configBuilder;

  @Before
  public void setUp() {
    this.sender = buildSender(getUrl());
    this.configBuilder = withAccessToken(ACCESS_TOKEN).sender(sender);

    stubFor(post(urlEqualTo("/api/1/item/"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"err\":0,\"result\":{\"uuid\":\"" + UUID.randomUUID() + "\"}}")));
  }

  @After
  public void tearDown() throws Exception {
    this.sender.close(true);
  }

  @Test
  public void builtInScrubbingRunsAfterTheUserTransformer() {
    // The user transformer injects the secret, so it can only be redacted if the built-in
    // scrubber runs afterwards.
    Transformer injectSecret = data -> new Data.Builder(data)
        .custom(objectMap("password", "hunter2", "user", "alice"))
        .build();

    Config config = configBuilder
        .transformer(injectSecret)
        .redactedKeys(Collections.singletonList("password"))
        .build();

    new Rollbar(config).error("boom");

    Map<String, Object> custom = getValue(sentData(0), "custom");
    assertThat(custom.get("password"), is(SCRUBBED));
    assertThat(custom.get("user"), is("alice"));
  }

  @Test
  public void reconfigurationChangesTheRedactedKeys() {
    // Keys outside the built-in list, so that only the reconfiguration can explain the change.
    Rollbar rollbar = new Rollbar(configBuilder
        .redactedKeys(Collections.singletonList("ssn"))
        .build());

    rollbar.error("boom", objectMap("ssn", "123-45-6789", "pin", "1234"));

    Map<String, Object> before = getValue(sentData(0), "custom");
    assertThat(before.get("ssn"), is(SCRUBBED));
    assertThat(before.get("pin"), is("1234"));

    rollbar.configure(configBuilder
        .redactedKeys(Collections.singletonList("pin"))
        .build());

    rollbar.error("boom", objectMap("ssn", "123-45-6789", "pin", "1234"));

    Map<String, Object> after = getValue(sentData(1), "custom");
    assertThat(after.get("ssn"), is("123-45-6789"));
    assertThat(after.get("pin"), is(SCRUBBED));
  }

  /**
   * The whole point of the built-in key list: a {@code /login?password=hunter2} request must not
   * leak the secret through any of the slots the request is serialized into, without the
   * application configuring anything.
   */
  @Test
  public void secretsAreRedactedFromEveryRequestRepresentationWithoutConfiguration() {
    Config config = configBuilder
        .request(() -> new Request.Builder()
            .url("https://example.com/login?password=hunter2")
            .method("POST")
            .headers(objectStringMap("Authorization", "Bearer hunter2", "Accept", "text/html"))
            .params(objectStringMap("password", "hunter2", "userId", "42"))
            .get(Collections.singletonMap("password", Collections.singletonList("hunter2")))
            .post(objectMap("password", "hunter2", "username", "alice"))
            .metadata(objectMap("access_token", "hunter2", "region", "us-east-1"))
            .queryString("password=hunter2")
            .build())
        .build();

    new Rollbar(config).error("boom", objectMap("password", "hunter2", "username", "alice"));

    // Nothing anywhere in the payload — telemetry, notifier metadata and all — carries the secret.
    assertThat(sentPayload(0).contains("hunter2"), is(false));

    Map<String, Object> request = getValue(sentData(0), "request");
    assertThat(request.get("url"), is("https://example.com/login"));
    assertThat(request.get("query_string"), is("password=" + SCRUBBED));
    assertThat(getValue(request, "get", "password"), is(SCRUBBED));
    assertThat(getValue(request, "post", "password"), is(SCRUBBED));
    assertThat(getValue(request, "params", "password"), is(SCRUBBED));
    assertThat(getValue(request, "headers", "Authorization"), is(SCRUBBED));
    // Request.metadata is flattened onto the request object itself.
    assertThat(request.get("access_token"), is(SCRUBBED));
    assertThat(getValue(sentData(0), "custom", "password"), is(SCRUBBED));

    // Non-sensitive siblings are untouched.
    assertThat(getValue(request, "post", "username"), is("alice"));
    assertThat(getValue(request, "params", "userId"), is("42"));
    assertThat(getValue(request, "headers", "Accept"), is("text/html"));
    assertThat(request.get("region"), is("us-east-1"));
    assertThat(getValue(sentData(0), "custom", "username"), is("alice"));
  }

  @Test
  public void defaultRedactedKeysCanBeTurnedOff() {
    Config config = configBuilder
        .useDefaultRedactedKeys(false)
        .build();

    new Rollbar(config).error("boom", objectMap("password", "hunter2"));

    assertThat(getValue(sentData(0), "custom", "password"), is("hunter2"));
  }

  @Test
  public void nestedCollectionsAreScrubbedEndToEnd() {
    Config config = configBuilder
        .redactedKeys(Collections.singletonList("password"))
        .build();

    Map<String, Object> custom = new HashMap<>();
    custom.put("users", Arrays.asList(objectMap("password", "hunter2"), objectMap("name", "bob")));
    custom.put("keys", new Object[] {objectMap("password", "hunter2")});

    new Rollbar(config).error("boom", custom);

    Map<String, Object> sentCustom = getValue(sentData(0), "custom");

    List<Map<String, Object>> users = getValue(sentCustom, "users");
    assertThat(users, hasSize(2));
    assertThat(users.get(0).get("password"), is(SCRUBBED));
    assertThat(users.get(1).get("name"), is("bob"));

    List<Map<String, Object>> keys = getValue(sentCustom, "keys");
    assertThat(keys, hasSize(1));
    assertThat(keys.get(0).get("password"), is(SCRUBBED));
  }

  @Test
  public void networkTelemetryUrlsAreSanitized() {
    Rollbar rollbar = new Rollbar(configBuilder.build());

    rollbar.recordNetworkEventFor(Level.CRITICAL, "GET",
        "https://user:pass@example.com/orders?token=secret#frag", "500");
    rollbar.error("boom");

    List<Map<String, Object>> telemetry = getValue(sentData(0), "body", "telemetry");
    assertThat(telemetry, hasSize(1));
    Map<String, Object> body = getValue(telemetry.get(0), "body");
    assertThat(body.get("url"), is("https://example.com/orders"));
  }

  // --- helpers ---

  protected Sender buildSender(String url) {
    return new SyncSender.Builder().url(url).accessToken(ScrubbingITest.ACCESS_TOKEN).build();
  }

  /** The raw JSON body of the nth payload WireMock received. */
  private String sentPayload(int index) {
    return WireMock.findAll(postRequestedFor(urlEqualTo("/api/1/item/")))
        .get(index).getBodyAsString();
  }

  /** The parsed {@code data} object of the nth payload WireMock received. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> sentData(int index) {
    Map<String, Object> payload = new Gson().fromJson(sentPayload(index), Map.class);
    return getValue(payload, "data");
  }

  @SuppressWarnings("unchecked")
  private static <T> T getValue(Map<String, Object> source, String attribute,
                                String... attributes) {
    Object value = source.get(attribute);

    if (attributes.length == 0) {
      return (T) value;
    }

    if (value == null) {
      throw new NullPointerException("No value with key " + attribute);
    }

    Map<String, Object> asMap = (Map<String, Object>) value;
    String[] newAttributes = new String[attributes.length - 1];
    System.arraycopy(attributes, 1, newAttributes, 0, newAttributes.length);

    return getValue(asMap, attributes[0], newAttributes);
  }

  private static Map<String, Object> objectMap(String... kvPairs) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < kvPairs.length; i += 2) {
      map.put(kvPairs[i], kvPairs[i + 1]);
    }
    return map;
  }

  private static Map<String, String> objectStringMap(String... kvPairs) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < kvPairs.length; i += 2) {
      map.put(kvPairs[i], kvPairs[i + 1]);
    }
    return map;
  }

  private String getUrl() {
    return format(Locale.US, "http://localhost:%d/api/1/item/", wireMockRule.port());
  }
}
