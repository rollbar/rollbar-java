package com.rollbar.payload.utilities;

import com.rollbar.payload.Payload;

public interface PayloadSerializer {
    String serialize(Payload payload);
}
