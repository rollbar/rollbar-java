package com.rollbar.http;

import com.rollbar.payload.Payload;

public interface PayloadSerializer {
    String serialize(Payload payload);
}
