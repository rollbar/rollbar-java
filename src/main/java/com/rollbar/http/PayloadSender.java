package com.rollbar.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rollbar.payload.Payload;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

import java.io.*;
import java.net.*;

/**
 * Sends payloads (synchronously) to Rollbar. This serves both as a reference implementation for {@link Sender}
 * and the default implementation.
 */
public class PayloadSender implements Sender {
    public static final String DEFAULT_API_ENDPOINT = "https://api.rollbar.com/api/1/item/";

    private final URL url;

    /**
     * Default constructor, sends to the public api endpoint.
     * @throws ArgumentNullException if url is null
     * @throws MalformedURLException if url is not a valid URL
     */
    public PayloadSender() {
        try {
            this.url = new URL(DEFAULT_API_ENDPOINT);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("The DEFAULT_API_ENDPOINT is valid. This should never get hit.");
        }
    }

    /**
     * Constructor
     * @param url The Rollbar endpoint to POST items to.
     * @throws ArgumentNullException if url is null
     * @throws MalformedURLException if url is not a valid URL
     */
    public PayloadSender(String url) throws ArgumentNullException, MalformedURLException {
        Validate.isNotNull(url, "url");
        this.url = new URL(url);
    }

    /**
     * Sends the json (rollbar payload) to the endpoint configured in the constructor.
     * Returns the (parsed) response from Rollbar.
     * @param json the serialized JSON payload
     * @return the response from Rollbar {@link RollbarResponse}
     * @throws ConnectionFailedException if the connection fails at any point along the way {@link ConnectionFailedException}
     * @throws UnsupportedEncodingException if json.getBytes("UTF-8") fails
     */
    public RollbarResponse send(String json) throws ConnectionFailedException {
        HttpURLConnection connection = getConnection();

        final byte[] bytes;
        try {
            bytes = json.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Rollbar Requires UTF8 Encoding and your JVM does not support UTF8, please update your JVM");
        }

        sendJson(connection, bytes);

        return readResponse(connection);
    }

    private RollbarResponse readResponse(HttpURLConnection connection) throws ConnectionFailedException {
        int result;
        String content;
        try {
            result = connection.getResponseCode();
            content = GetResponseContent(connection);

        } catch (IOException e) {
            throw new ConnectionFailedException(connection.getURL(), "Reading the Response Failed", e);
        }
        RollbarResponseCode code;
        try {
            code = RollbarResponseCode.fromInt(result);
        } catch (InvalidResponseCodeException e) {
            throw new ConnectionFailedException(connection.getURL(), "Unknown Response Code Received", e);
        }

        JsonParser parser = new JsonParser();
        JsonObject obj = (JsonObject)parser.parse(content);
        if (result >= 400) {
            String message = obj.get("message").getAsString();
            return code.response(message);
        } else {
            String uuid = obj.get("result").getAsJsonObject().get("uuid").getAsString();
            return RollbarResponse.success(uuid);
        }
    }

    private static String GetResponseContent(HttpURLConnection connection) throws IOException {
        final InputStream inputStream;
        if (connection.getResponseCode() == 200) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        final InputStreamReader reader = new InputStreamReader(inputStream,"utf-8");
        final BufferedReader bis = new BufferedReader(reader);
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = bis.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        bis.close();
        return buffer.toString();
    }

    private HttpURLConnection getConnection() throws ConnectionFailedException {
        HttpURLConnection connection = getHttpURLConnection();
        setMethodToPOST(connection);
        setJsonSendAndReceive(connection);
        return connection;
    }

    private void sendJson(HttpURLConnection connection, byte[] bytes) throws ConnectionFailedException {
        OutputStream out;
        try {
            out = connection.getOutputStream();
        } catch (IOException e) {
            throw new ConnectionFailedException(url, "OpeningBodyWriter", e);
        }
        try {
            out.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ConnectionFailedException(url, "WritingToBody", e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new ConnectionFailedException(url, "Closing Body Writer", e);
        }
    }

    private void setJsonSendAndReceive(HttpURLConnection connection) {
        connection.setRequestProperty("Accept-Charset", "utf-8");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
    }

    private void setMethodToPOST(HttpURLConnection connection) throws ConnectionFailedException {
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new ConnectionFailedException(url, "Setting method to POST Failed", e);
        }
        connection.setDoOutput(true);
    }

    private HttpURLConnection getHttpURLConnection() throws ConnectionFailedException {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new ConnectionFailedException(url, "Initializing URL Connection", e);
        }
        return connection;
    }
}
