package com.rollbar.http;

import com.rollbar.payload.Payload;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends payloads (synchronously) to Rollbar. This serves both as a reference implementation for {@link Sender}
 * and the default implementation.
 */
public class PayloadSender implements Sender {
    /**
     * If you don't set the url this is the URL that gets used.
     */
    public static final String DEFAULT_API_ENDPOINT = "https://api.rollbar.com/api/1/item/";

    private final URL url;

    /**
     * Default constructor, sends to the public api endpoint.
     * @throws ArgumentNullException if url is null
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
     * @param payload the serialized JSON payload
     * @return the response from Rollbar {@link RollbarResponse}
     */
    public RollbarResponse send(Payload payload) {
        HttpURLConnection connection = null;
        try {
            connection = getConnection();
        } catch (ConnectionFailedException e) {
            return RollbarResponse.failure(RollbarResponseCode.ConnectionFailed, e.getMessage());
        }

        final byte[] bytes;
        try {
            bytes = payload.toJson().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Rollbar Requires UTF8 Encoding and your JVM does not support UTF8, please update your JVM");
        }

        try {
            sendJson(connection, bytes);
        } catch (ConnectionFailedException e) {
            return RollbarResponse.failure(RollbarResponseCode.ConnectionFailed, e.getMessage());
        }

        try {
            return readResponse(connection);
        } catch (ConnectionFailedException e) {
            return RollbarResponse.failure(RollbarResponseCode.ConnectionFailed, e.getMessage());
        }
    }

    public void send(Payload payload, RollbarResponseHandler handler) {
        RollbarResponse response = send(payload);
        handler.handleResponse(response);
    }

    private static final Pattern messagePattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern uuidPattern = Pattern.compile("\"uuid\"\\s*:\\s*\"([^\"]*)\"");

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
        boolean err = result >= 400;
        Pattern p = err ? messagePattern : uuidPattern;
        Matcher m = p.matcher(content);
        m.find();

        if (err) {
            return code.response(m.group(1));
        } else {
            return RollbarResponse.success(m.group(1));
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
