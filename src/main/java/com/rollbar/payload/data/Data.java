package com.rollbar.payload.data;

import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.InvalidLengthException;
import com.rollbar.payload.utilities.Validate;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

/**
 * Represents the actual data being posted to Rollbar
 */
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

    /**
     * Constructor
     * @param environment not nullable, string representing the current environment (e.g.: production, debug, test)
     * @param body not nullable, the actual data being sent to rollbar (not metadata, about the request, server, etc.)
     * @throws ArgumentNullException if neither body or environment is null, or if environment is empty or whitespace
     * @throws InvalidLengthException if the environment is longer than 255 characters
     */
    public Data(String environment, Body body) throws ArgumentNullException, InvalidLengthException {
        this(environment, body, null, (Long) null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor
     * @param environment not nullable, string representing the current environment (e.g.: production, debug, test)
     * @param body not nullable, the actual data being sent to rollbar (not metadata, about the request, server, etc.)
     * @param level the rollbar error level
     * @param timestamp the moment the bug happened, visible in ui as client_timestamp
     * @param codeVersion the currently running version of the code
     * @param platform the platform running (most likely JVM and a version)
     * @param language the language running (most likely java, but any JVM language might be here)
     * @param framework the framework being run (e.g. Play, Spring, etc)
     * @param context custom identifier to help find where the error came from, Controller class name, for instance.
     * @param request data about the Http Request that caused this, if applicable
     * @param person data about the user that experienced the error, if possible
     * @param server data about the machine on which the error occurred
     * @param custom custom data that will aid in debugging the error
     * @param fingerprint override the default and custom grouping with a string, if over 255 characters will be hashed
     * @param title the title, max length 255 characters, overrides the default and custom ones set by rollbar
     * @param uuid override the error UUID, unique to each project, used to deduplicate occurrences
     * @param notifier information about this notifier, esp. if creating a framework specific notifier
     * @throws ArgumentNullException if environment or body is null
     * @throws InvalidLengthException if environment or title is over 255 characters, or uuid is over 32 characters
     */
    public Data(String environment, Body body, Level level, Instant timestamp, String codeVersion, String platform, String language, String framework, String context, Request request, Person person, Server server, HashMap<String, Object> custom, String fingerprint, String title, UUID uuid, Notifier notifier) throws ArgumentNullException, InvalidLengthException {
        this(environment, body, level, timestamp == null ? null : timestamp.getEpochSecond(), codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid == null ? null : uuid.toString(), notifier);
    }

    private Data(String environment, Body body, Level level, Long timestamp, String codeVersion, String platform, String language, String framework, String context, Request request, Person person, Server server, HashMap<String, Object> custom, String fingerprint, String title, String uuid, Notifier notifier) throws ArgumentNullException, InvalidLengthException {
        Validate.isNotNullOrWhitespace(environment, "environment");
        Validate.maxLength(environment, 255, "environment");
        Validate.isNotNull(body, "body");
        if (title != null) {
            Validate.maxLength(title, 255, "title");
        }
        if (uuid != null) {
            Validate.maxLength(uuid, 32, "uuid");
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

    /**
     * @return string representing the current environment (e.g.: production, debug, test)
     */
    public String environment() {
        return this.environment;
    }

    public Data environment(String environment) throws ArgumentNullException, InvalidLengthException {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return not nullable, the actual data being sent to rollbar (not metadata, about the request, server, etc.)
     */
    public Body body() {
        return this.body;
    }

    public Data body(Body body) throws ArgumentNullException {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return the rollbar error level
     */
    public Level level() {
        return this.level;
    }

    public Data level(Level level) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return the moment the bug happened, visible in ui as client_timestamp
     */
    public Instant timestamp() {
        return this.timestamp == null ? null : Instant.ofEpochSecond(this.timestamp);
    }

    public Data timestamp(Instant date) {
        return new Data(environment, body, level, date == null ? null : date.getEpochSecond(), codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return the currently running version of the code
     */
    public String codeVersion() {
        return this.codeVersion;
    }

    public Data codeVersion(String codeVersion) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return the platform running (most likely JVM and a version)
     */
    public String platform() {
        return this.platform;
    }

    public Data platform(String platform) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return the language running (most likely java, but any JVM language might be here)
     */
    public String language() {
        return this.language;
    }

    public Data language(String language) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return the framework being run (e.g. Play, Spring, etc)
     */
    public String framework() {
        return this.framework;
    }

    public Data framework(String framework) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return custom identifier to help find where the error came from, Controller class name, for instance.
     */
    public String context() {
        return this.context;
    }

    public Data context(String context) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return data about the Http Request that caused this, if applicable
     */
    public Request request() {
        return this.request;
    }

    public Data request(Request request) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return data about the user that experienced the error, if possible
     */
    public Person person() {
        return this.person;
    }

    public Data person(Person person) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return data about the machine on which the error occurred
     */
    public Server server() {
        return this.server;
    }

    public Data server(Server server) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return custom data that will aid in debugging the error
     */
    public HashMap<String, Object> custom() {
        return custom == null ? null : new HashMap<String, Object>(this.custom);
    }

    public Data custom(HashMap<String, Object> custom) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return override the default and custom grouping with a string, if over 255 characters will be hashed
     */
    public String fingerprint() {
        return this.fingerprint;
    }

    public Data fingerprint(String fingerprint) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return the title, max length 255 characters, overrides the default and custom ones set by rollbar
     */
    public String title() {
        return this.title;
    }

    public Data title(String title) throws InvalidLengthException {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }

    /**
     * @return override the error UUID, unique to each project, used to deduplicate occurrences
     */
    public UUID uuid() throws InvalidLengthException {
        return UUID.fromString(this.uuid);
    }

    public Data uuid(UUID uuid) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid == null ? null : uuid.toString(), notifier);
    }

    /**
     * @return information about this notifier, esp. if creating a framework specific notifier
     */
    public Notifier notifier() {
        return this.notifier;
    }

    public Data notifier(Notifier notifier) {
        return new Data(environment, body, level, timestamp, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier);
    }
}
