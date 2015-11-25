package com.rollbar.payload.data;

import com.rollbar.payload.utilities.Properties;

public class Notifier {
    public static final String defaultName = "rollbar";
    public static final String defaultVersion = Properties.getAssemblyVersion();

    private final String name;
    private final String version;

    public Notifier() {
        this.name = defaultName;
        this.version = defaultVersion;
    }

    public Notifier(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String name() {
        return this.name;
    }

    public Notifier name(String name) {
        return new Notifier(name, version);
    }

    public String version() {
        return this.version;
    }

    public Notifier version(String version) {
        return new Notifier(name, version);
    }
}
