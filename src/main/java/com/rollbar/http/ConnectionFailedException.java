package com.rollbar.http;

import java.net.URL;

public class ConnectionFailedException extends Throwable {
    public ConnectionFailedException(URL url, String reason, Exception e) {
        super(String.format("Could not connect to %s because %s failed", url.toString(), reason), e);
    }
}
