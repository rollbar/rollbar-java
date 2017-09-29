package com.rollbar.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.sender.json.JsonSerializerImpl;
import com.rollbar.notifier.sender.listener.SenderListener;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Synchronous implementation of the {@link Sender sender}.
 */
public class SyncSender implements Sender {

  public static final String DEFAULT_API_ENDPOINT = "https://api.rollbar.com/api/1/item/";

  private static final String UTF_8 = StandardCharsets.UTF_8.name();

  private final URL url;

  private final JsonSerializer serializer;

  private final List<SenderListener> listeners;

  /**
   * Constructor.
   */
  public SyncSender() {
    this(DEFAULT_API_ENDPOINT);
  }

  /**
   * Constructor.
   *
   * @param url the Rollbar API endpoint.
   */
  public SyncSender(String url) {
    this(url, new JsonSerializerImpl());
  }

  /**
   * Constructor.
   *
   * @param url the Rollbar API endpoint.
   * @param serializer the serializer.
   */
  public SyncSender(String url, JsonSerializer serializer) {
    try {
      this.url = new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("The url provided is not valid: " + url, e);
    }
    this.serializer = serializer;

    this.listeners = new ArrayList<>();
  }

  SyncSender(URL url, JsonSerializer serializer) {
    this.url = url;
    this.serializer = serializer;

    this.listeners = new ArrayList<>();
  }

  @Override
  public void send(Payload payload) {
    try {
      String json = serializer.toJson(payload);
      Result result = send(json);
      notifyResult(payload, result);
    } catch (Exception e) {
      notifyError(payload, new SenderException(e));
    }
  }

  @Override
  public void addListener(SenderListener listener) {
    this.listeners.add(listener);
  }

  private void notifyResult(Payload payload, Result result) {
    for (SenderListener listener : listeners) {
      listener.onResult(payload, result);
    }
  }

  private void notifyError(Payload payload, Exception error) {
    for (SenderListener listener : listeners) {
      listener.onError(payload, error);
    }
  }

  private Result send(String body) throws IOException {
    HttpURLConnection connection = getConnection();
    byte[] bytes = body.getBytes(UTF_8);
    sendJson(connection, bytes);
    return readResponse(connection);
  }

  private HttpURLConnection getConnection() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

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
    return serializer.resultFrom(resultCode, resultContent);
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
}

