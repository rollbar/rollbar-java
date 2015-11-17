package com.rollbar.http;

import java.io.IOException;
import java.net.URL;

/**
 * Created by chris on 11/13/15.
 */
public class ConnectionFailedException extends Throwable {
    public enum Reason {
        Initialization,
        WritingToBody,
        OpeningBodyWriter,
        ClosingBodyWriter,
        SettingPOSTFailed,
        ResponseReadingFailed, UnknownResponseCode,
    }
    public ConnectionFailedException(URL url, Reason reason, Exception e) {
        super(String.format("Could not connect to %s because %s failed", url.toString(), reason.toString()), e);
    }
}
