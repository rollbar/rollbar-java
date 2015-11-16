package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

public class CrashReport implements BodyContents {
    private final String raw;

    public CrashReport(String raw) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(raw, "raw");
        this.raw = raw;
    }

    public String raw() {
        return this.raw;
    }

    public CrashReport raw(String raw) throws ArgumentNullException {
        return new CrashReport(raw);
    }
}
