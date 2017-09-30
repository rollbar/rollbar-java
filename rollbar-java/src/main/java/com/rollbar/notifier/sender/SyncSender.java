package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.sender.json.JsonSerializerImpl;
import com.rollbar.notifier.sender.result.Result;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Synchronous implementation of the {@link Sender sender}.
 */
public class SyncSender extends AbstractSender {

  public static final String DEFAULT_API_ENDPOINT = "https://api.rollbar.com/api/1/item/";

  private static final String UTF_8 = StandardCharsets.UTF_8.name();

  private final URL url;

  private final JsonSerializer jsonSerializer;

  private final String accessToken;

  SyncSender(Builder builder) {
    this.url = builder.url;
    this.jsonSerializer = builder.jsonSerializer;
    this.accessToken = builder.accessToken;
  }

  @Override
  public Result doSend(Payload payload) throws Exception {
    String json = jsonSerializer.toJson(payload);
    return send(json);
  }

  private Result send(String body) throws IOException {
    HttpURLConnection connection = getConnection();
    byte[] bytes = body.getBytes(UTF_8);
    sendJson(connection, bytes);
    return readResponse(connection);
  }

  private HttpURLConnection getConnection() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    if (accessToken != null && !"".equals(accessToken)) {
      connection.setRequestProperty("x-rollbar-access-token", accessToken);
    }

    connection.setRequestProperty("Accept-Charset", UTF_8);
    connection.setRequestProperty("Content-Type", "application/json; charset=" + UTF_8);
    connection.setRequestProperty("Accept", "application/json");
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");

    return connection;
  }

  private void sendJson(HttpURLConnection connection, byte[] bytes) throws IOException {
    try (OutputStream out = connection.getOutputStream()) {
      out.write(bytes, 0, bytes.length);
    } catch (IOException e) {
      throw e;
    }
  }

  private Result readResponse(HttpURLConnection connection) throws IOException {
    int resultCode = connection.getResponseCode();
    String resultContent = getResponseContent(connection);
    return jsonSerializer.resultFrom(resultCode, resultContent);
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

  public static final class Builder {

    private URL url;

    private JsonSerializer jsonSerializer;

    private String accessToken;

    public Builder() {
      this.url = parseUrl(DEFAULT_API_ENDPOINT);
      this.jsonSerializer = new JsonSerializerImpl();
    }

    public Builder url(String url) {
      this.url = parseUrl(url);
      return this;
    }

    public Builder url(URL url) {
      this.url = url;
      return this;
    }

    public Builder jsonSerializer(JsonSerializer jsonSerializer) {
      this.jsonSerializer = jsonSerializer;
      return this;
    }

    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    public SyncSender build() {
      return new SyncSender(this);
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

