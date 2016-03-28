package com.rollbar.payload;

import com.rollbar.http.*;
import com.rollbar.payload.data.Data;
import com.rollbar.payload.data.Level;
import com.rollbar.payload.data.Notifier;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.RollbarSerializer;
import com.rollbar.payload.utilities.Validate;
import com.rollbar.payload.utilities.JsonSerializable;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the payload to send to Rollbar. A successfully constructed Payload matches Rollbar's spec, and should be
 * successful when serialized and POSTed to the correct endpoint.
 */
public final class Payload implements JsonSerializable {
    private static Sender sender = new PayloadSender();

    /**
     * Call to set the sender used by all Payloads when `send` is called.
     * Note: This should be thread safe. That is easiest when the sender keeps *no* instance data.
     * @param sender not nullable, the sender to use instead of the default
     */
    public static void setSender(Sender sender) {
        Validate.isNotNull(sender, "sender");
        Payload.sender = sender;
    }

    /**
     * A shortcut factory for creating a payload
     * @param accessToken not nullable, the server_post access token to send this payload to
     * @param environment not nullable, the environment the code is currently running under
     * @param error not nullable, the error being reported
     * @param custom any custom data to be sent (null is OK)
     * @return the payload
     */
    public static Payload fromError(String accessToken, String environment, Throwable error, LinkedHashMap<String, Object> custom) {
        Validate.isNotNullOrWhitespace(accessToken, "accessToken");
        Validate.isNotNullOrWhitespace(environment, "environment");
        Validate.isNotNull(error, "error");

        Body body = Body.fromError(error);
        Level level = error instanceof Error ? Level.CRITICAL : Level.ERROR;
        String platform = System.getProperty("java.version");
        Data d = new Data(environment, body, level, new Date(), null, platform, "java", null, null, null, null, null, custom, null, null, null, new Notifier());
        return new Payload(accessToken, d);
    }

    /**
     * A shortcut factory for creating a payload
     * @param accessToken not nullable, the server_post access token to send this payload to
     * @param environment not nullable, the environment the code is currently running under
     * @param message not nullable, the message to log to Rollbar
     * @param custom any custom data to be sent (null is OK)
     * @return the payload
     */
    public static Payload fromMessage(String accessToken, String environment, String message, LinkedHashMap<String, Object> custom) {
        Validate.isNotNullOrWhitespace(accessToken, "accessToken");
        Validate.isNotNullOrWhitespace(environment, "environment");
        Validate.isNotNull(message, "message");

        Body body = Body.fromString(message, custom);
        String platform = System.getProperty("java.version");
        Data d = new Data(environment, body, Level.WARNING, new Date(), null, platform, "java", null, null, null, null, null, null, null, null, null, new Notifier());
        return new Payload(accessToken, d);
    }

    private final String accessToken;
    private final Data data;

    /**
     * Constructor
     * @param accessToken An access token with scope "post_server_item" or "post_client_item". Probably "server" unless
     *                    your {@link Data#platform()} is "android" or "client". Must not be null or whitespace.
     * @param data The data to POST to Rollbar. Must not be null.
     * @throws ArgumentNullException if either argument was null
     */
    public Payload(String accessToken, Data data) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(accessToken, "accessToken");
        this.accessToken = accessToken;

        Validate.isNotNull(data, "data");
        this.data = data;
    }

    /**
     * @return the access token
     */
    public String accessToken() {
        return accessToken;
    }

    /**
     * @return the data
     */
    public Data data() {
        return data;
    }

    /**
     * Set the access token
     * @param token the new access token
     * @return a copy of this Payload with the token overridden
     * @throws ArgumentNullException if {@code token} is null
     */
    public Payload accessToken(String token) throws ArgumentNullException {
        return new Payload(token, this.data);
    }

    /**
     * Set the data
     * @param data the new data
     * @return a copy of this Payload with the data overridden
     * @throws ArgumentNullException if {@code data} is null
     */
    public Payload data(Data data) throws ArgumentNullException {
        return new Payload(this.accessToken, data);
    }

    /**
     * Convert this object to JSON that can be sent to Rollbar
     * @return the json representation of this object
     */
    public String toJson() {
        return new RollbarSerializer().serialize(this);
    }

    /**
     * Send this payload to Rollbar by the default Sender and Serializer
     * @return the response from Rollbar
     */
    public RollbarResponse send() {
        return sender.send(this);
    }

    /**
     * Send this payload to Rollbar. Handle the response with the handler.
     * @param handler the handler for
     */
    public void send(RollbarResponseHandler handler) {
        sender.send(this, handler);
    }

    public Map<String, Object> asJson() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("access_token", accessToken());
        obj.put("data", data());
        return obj;
    }
}
