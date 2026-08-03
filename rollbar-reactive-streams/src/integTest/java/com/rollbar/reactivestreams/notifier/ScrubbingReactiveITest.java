package com.rollbar.reactivestreams.notifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.rollbar.reactivestreams.notifier.config.ConfigBuilder.withAccessToken;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.Gson;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.scrubbing.ScrubDataTransformer;
import com.rollbar.notifier.transformer.Transformer;
import com.rollbar.reactivestreams.notifier.config.Config;
import com.rollbar.reactivestreams.notifier.config.ConfigBuilder;
import com.rollbar.reactivestreams.notifier.sender.AsyncSender;
import com.rollbar.reactivestreams.notifier.sender.http.ApacheAsyncHttpClient;
import com.rollbar.notifier.sender.result.Response;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The reactive {@link ConfigBuilder} duplicates the {@code redactedKeys} and {@code urlSanitizer}
 * plumbing of the synchronous one, so it needs its own end-to-end coverage. The scrubbing itself
 * is inherited from {@code RollbarBase} and is exercised in depth by
 * {@code com.rollbar.notifier.ScrubbingITest}.
 */
public class ScrubbingReactiveITest {

  private static final String ACCESS_TOKEN = UUID.randomUUID().toString();

  private static final String SCRUBBED = ScrubDataTransformer.SCRUBBED_VALUE;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  private ConfigBuilder configBuilder;

  @Before
  public void setUp() {
    AsyncSender sender = new AsyncSender.Builder(new ApacheAsyncHttpClient.Builder().build(),
        getUrl())
        .accessToken(ACCESS_TOKEN)
        .build();

    this.configBuilder = withAccessToken(ACCESS_TOKEN).sender(sender);

    stubFor(post(urlEqualTo("/api/1/item/"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"err\":0,\"result\":{\"uuid\":\"" + UUID.randomUUID() + "\"}}")));
  }

  @Test
  public void redactedKeysFromTheReactiveBuilderAreAppliedAfterTheUserTransformer()
      throws Exception {
    Transformer injectSecret = data -> new Data.Builder(data)
        .custom(nestedCustom())
        .build();

    Config config = configBuilder
        .transformer(injectSecret)
        .redactedKeys(Collections.singletonList("password"))
        .build();

    try (Rollbar rollbar = new Rollbar(config)) {
      await(rollbar.error("boom"));
    }

    Map<String, Object> custom = getValue(sentData(), "custom");
    assertThat(custom.get("password"), is(SCRUBBED));

    List<Map<String, Object>> users = getValue(custom, "users");
    assertThat(users, hasSize(1));
    assertThat(users.get(0).get("password"), is(SCRUBBED));
  }

  @Test
  public void networkTelemetryUrlsAreSanitized() throws Exception {
    try (Rollbar rollbar = new Rollbar(configBuilder.build())) {
      rollbar.recordNetworkEventFor(Level.CRITICAL, "GET",
          "https://user:pass@example.com/orders?token=secret", "500");
      await(rollbar.error("boom"));
    }

    List<Map<String, Object>> telemetry = getValue(sentData(), "body", "telemetry");
    assertThat(telemetry, hasSize(1));
    Map<String, Object> body = getValue(telemetry.get(0), "body");
    assertThat(body.get("url"), is("https://example.com/orders"));
  }

  // --- helpers ---

  private static Map<String, Object> nestedCustom() {
    Map<String, Object> custom = new HashMap<>();
    custom.put("password", "hunter2");
    Map<String, Object> user = new HashMap<>();
    user.put("password", "hunter2");
    custom.put("users", List.of(user));
    return custom;
  }

  private static void await(Publisher<Response> publisher) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    publisher.subscribe(new Subscriber<>() {
        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Response response) {
        }

        @Override
        public void onError(Throwable throwable) {
            latch.countDown();
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }
    });
    assertTrue("Timed out waiting for the payload to be sent", latch.await(20, TimeUnit.SECONDS));
  }

  /** The parsed {@code data} object of the nth payload WireMock received. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> sentData() {
    List<LoggedRequest> requests =
        WireMock.findAll(postRequestedFor(urlEqualTo("/api/1/item/")));
    Map<String, Object> payload =
        new Gson().fromJson(requests.get(0).getBodyAsString(), Map.class);
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

  private String getUrl() {
    return format(Locale.US, "http://localhost:%d/api/1/item/", wireMockRule.port());
  }
}
