package com.rollbar.reactivestreams.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.sender.json.JsonSerializerImpl;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.sender.result.Result;
import com.rollbar.reactivestreams.Utils;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpClient;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpRequest;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import org.reactivestreams.Publisher;

/**
 * Asynchronous, non-blocking sender based on the Reactive Streams specification.
 */
public class AsyncSender implements Sender {
  private final AsyncHttpClient httpClient;
  private final String url;
  private final JsonSerializer jsonSerializer;
  private final String accessToken;

  AsyncSender(Builder builder) {
    this.httpClient = builder.httpClient;
    this.url = builder.url.toExternalForm();
    this.jsonSerializer = builder.jsonSerializer;
    this.accessToken = builder.accessToken;
  }

  /**
   * Sends the payload.
   *
   * @param payload the payload.
   * @return A {@link Publisher} that will execute the operation once a subscriber requests it.
   */
  @Override
  public Publisher<Response> send(Payload payload) {
    LinkedHashMap<String, String> headers = new LinkedHashMap<>();

    if (accessToken != null && !"".equals(accessToken)) {
      headers.put("x-rollbar-access-token", accessToken);
    }

    headers.put("Accept-Charset", SyncSender.UTF_8);
    headers.put("Content-Type", "application/json; charset=" + SyncSender.UTF_8);
    headers.put("Accept", "application/json");

    String reqBody = jsonSerializer.toJson(payload);

    AsyncHttpRequest request =
        AsyncHttpRequest.Builder.build(this.url, headers.entrySet(), reqBody);

    return Utils.map(httpClient.send(request),
        new Utils.Converter<AsyncHttpResponse, Response>() {
          @Override
          public Response convert(AsyncHttpResponse from) {
            Result result = jsonSerializer.resultFrom(from.getBody());
            return new Response.Builder().result(result).status(from.getStatusCode()).build();
          }
        });
  }

  @Override
  public void close(boolean wait) {
    httpClient.close(wait);
  }

  @Override
  public void close() throws Exception {
    this.close(false);
  }

  /**
   * Builder class for {@link AsyncSender}.
   */
  public static class Builder {
    private final AsyncHttpClient httpClient;
    private URL url;
    private JsonSerializer jsonSerializer;
    private String accessToken;

    /**
     * Constructor.
     *
     * @param httpClient The async HTTP client to use.
     */
    public Builder(AsyncHttpClient httpClient) {
      this(httpClient, SyncSender.DEFAULT_API_ENDPOINT);
    }

    /**
     * Constructor.
     *
     * @param httpClient The async HTTP client to use.
     * @param url the url.
     */
    public Builder(AsyncHttpClient httpClient, String url) {
      this.httpClient = httpClient;
      this.url = parseUrl(url);
      this.jsonSerializer = new JsonSerializerImpl();
    }

    /**
     * The url as string.
     *
     * @param url the url.
     * @return the builder instance.
     */
    public Builder url(String url) {
      this.url = parseUrl(url);
      return this;
    }

    /**
     * The url as {@link URL}.
     *
     * @param url the url.
     * @return the builder instance.
     */
    public Builder url(URL url) {
      this.url = url;
      return this;
    }

    /**
     * The {@link JsonSerializer json serializer}.
     *
     * @param jsonSerializer the json serializer.
     * @return the builder instance.
     */
    public Builder jsonSerializer(JsonSerializer jsonSerializer) {
      this.jsonSerializer = jsonSerializer;
      return this;
    }

    /**
     * The rollbar access token.
     *
     * @param accessToken the access token.
     * @return the builder instance.
     */
    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    /**
     * Builds the {@link AsyncSender} async sender.
     *
     * @return the async sender.
     */
    public AsyncSender build() {
      return new AsyncSender(this);
    }

    /**
     * Builds a {@link Sender} that will execute all operations in fire-and-forget mode.
     * <p>
     *   All calls are delegated to an async sender, so this provides a quick migration path if
     *   an application is using non-blocking IO but is currently calling the synchronous
     *   {@link com.rollbar.notifier.Rollbar} methods.
     * </p>
     * <p>
     *   Note that the registered {@link com.rollbar.notifier.sender.listener.SenderListener}
     *   listeners will still be called, and if they perform blocking IO they might cause severe
     *   performance degradation for the non-blocking IO environment.
     * </p>
     *
     * @return the sync sender.
     */
    public com.rollbar.notifier.sender.Sender buildSync() {
      return new SyncSenderWrapper(this);
    }

    private static URL parseUrl(String url) {
      try {
        return new URL(url);
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException("The url provided is not valid: " + url, e);
      }
    }
  }
}
