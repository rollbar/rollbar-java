package com.rollbar.http;

import java.io.IOException;

public interface Sender {
    RollbarResponse Send(String jsonPayload) throws IOException, ConnectionFailedException;
}
