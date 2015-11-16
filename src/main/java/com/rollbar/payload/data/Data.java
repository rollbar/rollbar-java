package com.rollbar.payload.data;

import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.InvalidLengthException;
import com.rollbar.payload.utilities.Validate;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class Data {
    private final String environment;
    private final Body body;
    private final Level level;
    private final Long timestamp;
    private final String codeVersion;
    private final String platform;
    private final String language;
    private final String framework;
    private final String context;
    private final Request request;
    private final Person person;
    private final Server server;
    private final HashMap<String, Object> custom;
    private final String fingerprint;
    private final String title;
    private final String uuid;
    private final Notifier notifier;

    public Data(String environment, Body body) throws ArgumentNullException, InvalidLengthException {
        this(environment, body, null, (Long) null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Data(String environment, Body body, Level level, Date date, String codeVersion, String platform, String language, String framework, String context, Request request, Person person, Server server, HashMap<String, Object> custom, String fingerprint, String title, UUID uuid, Notifier notifier) throws ArgumentNullException, InvalidLengthException {
        this(environment, body, level, date == null ? null : (date.getTime() / 1000), codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid == null ? null : uuid.toString(), notifier);
    }

    private Data(String environment, Body body, Level level, Long timestamp, String codeVersion, String platform, String language, String framework, String context, Request request, Person person, Server server, HashMap<String, Object> custom, String fingerprint, String title, String uuid, Notifier notifier) throws ArgumentNullException, InvalidLengthException {
        Validate.isNotNullOrWhitespace(environment, "environment");
        Validate.maxLength(environment, 255, "environment");
        Validate.isNotNull(body, "body");
        if (title != null) {
            Validate.maxLength(title, 255, "title");
        }

        this.environment = environment;
        this.body = body;
        this.level = level;
        this.timestamp = timestamp;
        this.codeVersion = codeVersion;
        this.platform = platform;
        this.language = language;
        this.framework = framework;
        this.context = context;
        this.request = request;
        this.person = person;
        this.server = server;
        this.custom = custom == null ? null : new HashMap<String, Object>(custom);
        this.fingerprint = fingerprint;
        this.title = title;
        this.uuid = uuid;
        this.notifier = notifier;
    }

    public String environment() {
        return this.environment;
    }

    public Data environment(String environment) throws ArgumentNullException, InvalidLengthException {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    public Body body() {
        return this.body;
    }

    public Data body(Body body) throws ArgumentNullException {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("body can't be null");
        }
    }

    public Level level() {
        return this.level;
    }

    public Data level(Level level) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public Long timestamp() {
        return this.timestamp;
    }

    public Data timestamp(Date date) {
        try {
            return new Data(environment, body, level, date == null ? null : date.getTime() / 1000, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public String codeVersion() {
        return this.codeVersion;
    }

    public Data codeVersion(String codeVersion) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public String platform() {
        return this.platform;
    }

    public Data platform(String platform) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public String language() {
        return this.language;
    }

    public Data language(String language) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public String framework() {
        return this.framework;
    }

    public Data framework(String framework) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public String context() {
        return this.context;
    }

    public Data context(String context) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public Request request() {
        return this.request;
    }

    public Data request(Request request) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public Person person() {
        return this.person;
    }

    public Data person(Person person) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public Server server() {
        return this.server;
    }

    public Data server(Server server) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public HashMap<String, Object> custom() {
        return custom == null ? null : new HashMap<String, Object>(this.custom);
    }

    public Data custom(HashMap<String, Object> custom) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public String fingerprint() {
        return this.fingerprint;
    }

    public Data fingerprint(String fingerprint) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public String title() {
        return this.title;
    }

    public Data title(String title) throws InvalidLengthException {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("body and environment can't be null!");
        }
    }

    public UUID uuid() {
        return UUID.fromString(this.uuid);
    }

    public Data uuid(UUID uuid) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid == null ? null : uuid.toString(), notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }

    public Notifier notifier() {
        return this.notifier;
    }

    public Data notifier(Notifier notifier) {
        try {
            return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("environment and body can't null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("title and environment can't exceed 255 characters");
        }
    }
}
