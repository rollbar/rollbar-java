package com.rollbar.payload;

import com.rollbar.payload.data.Data;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

public final class Payload {
    private final String accessToken;
    private final Data data;

    public Payload(String accessToken, Data data) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(accessToken, "accessToken");
        this.accessToken = accessToken;

        Validate.isNotNull(data, "data");
        this.data = data;
    }

    public String accessToken() {
        return accessToken;
    }

    public Data data() {
        return data;
    }

    public Payload accessToken(String token) throws ArgumentNullException {
        return new Payload(token, this.data);
    }

    public Payload data(Data data) throws ArgumentNullException {
        return new Payload(this.accessToken, data);
    }
}