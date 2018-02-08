package com.rollbar.notifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;
import static java.lang.String.format;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Message;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.util.ConstantTimeProvider;
import com.rollbar.notifier.util.RollbarResponse;
import com.rollbar.notifier.util.SenderAssertions;
import com.rollbar.notifier.util.json.BodySerializer;
import com.rollbar.notifier.util.json.DataSerializer;
import com.rollbar.notifier.util.json.LevelSerializer;
import com.rollbar.notifier.util.json.PayloadSerializer;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RollbarITest {

  private static final int PORT = 8089;

  private static final String URL = format("http://localhost:%d/api/1/item/", PORT);

  private static final String ACCESS_TOKEN = UUID.randomUUID().toString();

  private static final String ERROR_MESSAGE = "This is an error message.";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  private Gson gson;

  private ConfigBuilder configBuilder;

  private Provider<Long> timeProvider;

  private Sender sender;

  @Before
  public void setUp() {
    LevelSerializer levelSerializer = new LevelSerializer();
    BodySerializer bodySerializer = new BodySerializer();
    PayloadSerializer payloadSerializer = new PayloadSerializer();
    DataSerializer dataSerializer = new DataSerializer();

    this.gson = new GsonBuilder()
        .registerTypeAdapter(Level.class, levelSerializer)
        .registerTypeAdapter(Body.class, bodySerializer)
        .registerTypeAdapter(Payload.class, payloadSerializer)
        .registerTypeAdapter(Data.class, dataSerializer)
        .create();

    this.timeProvider = new ConstantTimeProvider();

    this.sender = new SyncSender.Builder()
        .url(URL)
        .accessToken(ACCESS_TOKEN)
        .build();

    this.configBuilder = withAccessToken(ACCESS_TOKEN)
        .sender(sender)
        .timestamp(timeProvider);
  }

  @Test
  public void shouldSendSuccessfullyMessagePayload() {
    final String uuid = UUID.randomUUID().toString();

    Config config = configBuilder.build();

    Message message = new Message.Builder()
        .body(ERROR_MESSAGE)
        .build();

    Body body = new Body.Builder()
        .bodyContent(message)
        .build();

    Data data = new Data.Builder()
        .level(Level.ERROR)
        .body(body)
        .notifier(config.notifier().provide())
        .timestamp(timeProvider.provide())
        .language(config.language())
        .build();

    Payload payload = new Payload.Builder()
        .accessToken(ACCESS_TOKEN)
        .data(data)
        .build();

    stubFor(post(urlEqualTo("/api/1/item/"))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Accept-Charset", equalTo("UTF-8"))
        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
        .withHeader("x-rollbar-access-token", equalTo(ACCESS_TOKEN))
        .withRequestBody(equalToJson(gson.toJson(payload)))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(RollbarResponse.success(uuid)))));

    sender.addListener(SenderAssertions.assertResponseSuccess(uuid));

    Rollbar rollbar = new Rollbar(config);

    rollbar.error(ERROR_MESSAGE);
  }

  @Test
  public void shouldNotifyAccessTokenRequiredError() {
    int responseCode = 400;
    String errorMessage = "access token required";

    Sender sender = new SyncSender.Builder()
        .url(URL)
        .build();

    Config config = configBuilder
        .accessToken(null)
        .sender(sender)
        .build();

    Message message = new Message.Builder()
        .body(ERROR_MESSAGE)
        .build();

    Body body = new Body.Builder()
        .bodyContent(message)
        .build();

    Data data = new Data.Builder()
        .level(Level.ERROR)
        .body(body)
        .notifier(config.notifier().provide())
        .timestamp(timeProvider.provide())
        .language(config.language())
        .build();

    Payload payload = new Payload.Builder()
        .data(data)
        .build();

    stubFor(post(urlEqualTo("/api/1/item/"))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Accept-Charset", equalTo("UTF-8"))
        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
        .withRequestBody(equalToJson(gson.toJson(payload)))
        .willReturn(aResponse()
            .withStatus(responseCode)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(RollbarResponse.error(errorMessage)))));

    sender.addListener(SenderAssertions.assertApiError(responseCode, errorMessage));

    Rollbar rollbar = new Rollbar(config);

    rollbar.error(ERROR_MESSAGE);
  }
}
