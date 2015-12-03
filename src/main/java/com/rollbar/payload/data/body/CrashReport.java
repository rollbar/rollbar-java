package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

/**
 * Represents a crash report (currently only for iOS, eventually Android, and maybe (if possible) core and memory dumps)
 */
public class CrashReport implements BodyContents {
    private final String raw;

    /**
     * Constructor
     * @param raw the crash report string
     * @throws ArgumentNullException if raw is null
     */
    public CrashReport(String raw) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(raw, "raw");
        this.raw = raw;
    }

    /**
     * @return the crash report string
     */
    public String raw() {
        return this.raw;
    }

    /**
     * Set the raw string in a copy of this CrashReport
     * @param raw the new crash report string
     * @return a copy of this CrashReport with raw overridden
     * @throws ArgumentNullException if raw is null
     */
    public CrashReport raw(String raw) throws ArgumentNullException {
        return new CrashReport(raw);
    }
}
