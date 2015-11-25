package com.rollbar.http;

import java.net.URL;

/**
 * Represents a failure to send a payload because of a connection failure.
 */
public class ConnectionFailedException extends Throwable {
    /**
     * Constructor
     * @param url the url to which the connection failed
     * @param reason the reason why the connection failed in plain english
     * @param e the exception, if any, that caused the connection to fail
     */
    public ConnectionFailedException(URL url, String reason, Exception e) {
        super(String.format("Could not connect to %s because %s failed", url.toString(), reason), e);
    }
}
