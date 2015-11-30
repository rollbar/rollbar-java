package com.rollbar.payload;

import com.rollbar.payload.data.Data;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

/**
 * Represents the payload to send to Rollbar. A successfully constructed Payload matches Rollbar's spec, and should be
 * successful when serialized and POSTed to the correct endpoint.
 */
public final class Payload {
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
     * Get the access token
     * @return the access token
     */
    public String accessToken() {
        return accessToken;
    }

    /**
     * Get the Data
     * @return the data
     */
    public Data data() {
        return data;
    }

    /**
     * Set the access token on a clone of this object
     * @param token the token to override this one with
     * @return a copy of this Payload with the token overridden
     * @throws ArgumentNullException if {@code token} is null
     */
    public Payload accessToken(String token) throws ArgumentNullException {
        return new Payload(token, this.data);
    }

    /**
     * Set the data on a clone of this object
     * @param data the data to override this one with
     * @return a copy of this Payload with the token overriden
     * @throws ArgumentNullException if {@code data} is null
     */
    public Payload data(Data data) throws ArgumentNullException {
        return new Payload(this.accessToken, data);
    }
}