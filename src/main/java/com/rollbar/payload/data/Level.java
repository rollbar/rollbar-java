package com.rollbar.payload.data;

import com.google.gson.annotations.SerializedName;

/**
 * The Level of a Rollbar Report.
 */
public enum Level implements Comparable<Level> {
    @SerializedName("critical")
    CRITICAL,

    @SerializedName("error")
    ERROR,

    @SerializedName("warning")
    WARNING,

    @SerializedName("info")
    INFO,

    @SerializedName("debug")
    DEBUG,
}
