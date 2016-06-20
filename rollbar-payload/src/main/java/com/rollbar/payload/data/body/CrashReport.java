package com.rollbar.payload.data.body;

import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.JsonSerializable;
import com.rollbar.utilities.Validate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a crash report (currently only for iOS, eventually Android, and maybe (if possible) core and memory dumps)
 */
public class CrashReport implements BodyContents, JsonSerializable {
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

    public Map<String, Object> asJson() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("raw", raw());
        return obj;
    }

    public String getKeyName() {
        return "crash_report";
    }
}
