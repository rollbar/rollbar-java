package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.sender.json.JsonSerializerImpl;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.sender.result.Result;
import com.rollbar.notifier.util.ObjectsUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 * Synchronous implementation of the {@link Sender sender}.
 */
public class SyncSender extends AbstractSender {

  public static final String DEFAULT_API_ENDPOINT = "https://api.rollbar.com/api/1/item/";

  public static final String UTF_8 = "UTF-8";

  /**
   * Default time in milliseconds to wait while establishing a connection to the Rollbar API.
   */
  public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 30_000;

  /**
   * Default time in milliseconds to wait for data from the Rollbar API once connected.
   */
  public static final int DEFAULT_READ_TIMEOUT_MILLIS = 30_000;

  private final URL url;

  private final JsonSerializer jsonSerializer;

  private final String accessToken;

  private final Proxy proxy;

  private final boolean compressPayload;

  private final int connectTimeoutMillis;

  private final int readTimeoutMillis;

  SyncSender(Builder builder) {
    this.url = builder.url;
    this.jsonSerializer = builder.jsonSerializer;
    this.accessToken = builder.accessToken;
    this.proxy = builder.proxy != null ? builder.proxy : Proxy.NO_PROXY;
    this.compressPayload = builder.compressPayload;
    this.connectTimeoutMillis = builder.connectTimeoutMillis;
    this.readTimeoutMillis = builder.readTimeoutMillis;
  }

  @Override
  public Response doSend(Payload payload) throws Exception {
    String json = jsonSerializer.toJson(payload);
    return send(json);
  }

  @Override
  public void close() throws IOException {
    getConnection().disconnect();
  }

  private Response send(String body) throws IOException {
    HttpURLConnection connection = getConnection();
    byte[] bytes = body.getBytes(UTF_8);
    sendJson(connection, bytes);
    return readResponse(connection);
  }

  private HttpURLConnection getConnection() throws IOException {
    // By default, this will use the HTTPS Rollbar API URL defined in DEFAULT_API_ENDPOINT.
    HttpURLConnection connection = (HttpURLConnection) url.openConnection(this.proxy);

    // Without these calls HttpURLConnection never times out, which could block the sender thread forever.
    connection.setConnectTimeout(connectTimeoutMillis);
    connection.setReadTimeout(readTimeoutMillis);

    if (accessToken != null && !"".equals(accessToken)) {
      connection.setRequestProperty("x-rollbar-access-token", accessToken);
    }

    connection.setRequestProperty("Accept-Charset", UTF_8);
    connection.setRequestProperty("Content-Type", "application/json; charset=" + UTF_8);
    connection.setRequestProperty("Accept", "application/json");
    if (compressPayload) {
      connection.setRequestProperty("Content-Encoding", "gzip");
    }
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");

    return connection;
  }

  private void sendJson(HttpURLConnection connection, byte[] bytes) throws IOException {
    OutputStream out = null;
    try {
      OutputStream raw = connection.getOutputStream();
      try {
        out = compressPayload ? new GZIPOutputStream(raw) : raw;
      } catch (IOException e) {
        ObjectsUtils.close(raw);
        throw e;
      }
      out.write(bytes, 0, bytes.length);
    } finally {
      ObjectsUtils.close(out);
    }
  }

  Response readResponse(HttpURLConnection connection) throws IOException {
    int status = connection.getResponseCode();
    String content = getResponseContent(connection);
    Result result = jsonSerializer.resultFrom(content);
    return new Response.Builder()
        .status(status)
        .result(result)
        .build();
  }

  private static String getResponseContent(HttpURLConnection connection) throws IOException {
    final InputStream inputStream;
    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
      inputStream = connection.getInputStream();
    } else {
      inputStream = connection.getErrorStream();
    }
    final InputStreamReader reader = new InputStreamReader(inputStream, UTF_8);
    final BufferedReader bis = new BufferedReader(reader);
    StringBuilder buffer = new StringBuilder();
    String line;
    while ((line = bis.readLine()) != null) {
      if (buffer.length() != 0) {
        buffer.append("\n");
      }
      buffer.append(line);
    }
    bis.close();
    return buffer.toString();
  }

  /**
   * Builder class for {@link SyncSender}.
   */
  public static final class Builder {

    private URL url;

    private JsonSerializer jsonSerializer;

    private String accessToken;

    private Proxy proxy;

    private boolean compressPayload = true;

    private int connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;

    private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

    public Builder() {
      this(DEFAULT_API_ENDPOINT);
    }

    /**
     * Constructor.
     * @param url the url.
     */
    public Builder(String url) {
      this.url = parseUrl(url);
      this.jsonSerializer = new JsonSerializerImpl();
      this.proxy = null;
    }

    /**
     * The url as string.
     * @param url the url
     * @return the builder instance.
     */
    public Builder url(String url) {
      this.url = parseUrl(url);
      return this;
    }

    /**
     * The url as {@link URL}.
     * @param url the url
     * @return the builder instance.
     */
    public Builder url(URL url) {
      this.url = url;
      return this;
    }

    /**
     * The {@link JsonSerializer json serializer}.
     * @param jsonSerializer the json serializer.
     * @return the builder instance.
     */
    public Builder jsonSerializer(JsonSerializer jsonSerializer) {
      this.jsonSerializer = jsonSerializer;
      return this;
    }

    /**
     * The rollbar access token.
     * @param accessToken the access token.
     * @return the builder instance.
     */
    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    /**
     * The {@link Proxy proxy}.
     * @param proxy the proxy.
     * @return the builder instance.
     */
    public Builder proxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Whether to gzip-compress payloads before sending. Default: true.
     * @param compress true to enable compression.
     * @return the builder instance.
     */
    public Builder compressPayload(boolean compress) {
      this.compressPayload = compress;
      return this;
    }

    /**
     * Time in milliseconds to wait while establishing a connection to the Rollbar API.
     * Default: {@value SyncSender#DEFAULT_CONNECT_TIMEOUT_MILLIS}. Zero means wait forever, which
     * is not recommended: a single unresponsive connection would stop all reporting.
     * @param connectTimeoutMillis the connect timeout in milliseconds.
     * @return the builder instance.
     * @throws IllegalArgumentException if the timeout is negative.
     */
    public Builder connectTimeoutMillis(int connectTimeoutMillis) {
      this.connectTimeoutMillis = requireNonNegative(connectTimeoutMillis, "connectTimeoutMillis");
      return this;
    }

    /**
     * Time in milliseconds to wait for data from the Rollbar API once connected.
     * Default: {@value SyncSender#DEFAULT_READ_TIMEOUT_MILLIS}. Zero means wait forever, which
     * is not recommended: a single unresponsive connection would stop all reporting.
     * @param readTimeoutMillis the read timeout in milliseconds.
     * @return the builder instance.
     * @throws IllegalArgumentException if the timeout is negative.
     */
    public Builder readTimeoutMillis(int readTimeoutMillis) {
      this.readTimeoutMillis = requireNonNegative(readTimeoutMillis, "readTimeoutMillis");
      return this;
    }

    /**
     * Builds the {@link SyncSender sync sender}.
     *
     * @return the sync sender.
     */
    public SyncSender build() {
      return new SyncSender(this);
    }

    private static int requireNonNegative(int value, String name) {
      if (value < 0) {
        throw new IllegalArgumentException(name + " must not be negative: " + value);
      }
      return value;
    }

    private static URL parseUrl(String url) {
      try {
        URL result = new URL(url);
        return result;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException("The url provided is not valid: " + url, e);
      }
    }
  }
}

