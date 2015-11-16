package com.rollbar.payload.data;

import com.google.gson.annotations.SerializedName;

public enum Level {
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
