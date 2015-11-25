package com.rollbar.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rollbar.payload.Payload;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

import java.io.*;
import java.net.*;

public class PayloadSender implements Sender {
    private final URL url;

    public PayloadSender(String url) throws ArgumentNullException, MalformedURLException {
        Validate.isNotNull(url, "url");
        this.url = new URL(url);
    }

    public RollbarResponse Send(String json) throws ConnectionFailedException, UnsupportedEncodingException {
        HttpURLConnection connection = getConnection();

        sendJson(connection, json.getBytes("UTF-8"));

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
