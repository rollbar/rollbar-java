package com.rollbar;

import com.rollbar.http.*;
import com.rollbar.payload.Payload;
import com.rollbar.payload.data.*;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.utilities.Validate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Override getters to dynamically produce the members.
 * Setters are private for IOC containers to use.
 * All setters named without `get` return a copy of this with the value overridden.
 */
public class Rollbar {
    private String accessToken;
    private String environment;
    private String codeVersion;
    private String platform;
    private String language;
    private String framework;
    private String context;
    private Request request;
    private Person person;
    private Server server;
    private Map<String, Object> custom;
    private String fingerprint;
    private String title;
    private UUID uuid;
    private Notifier notifier;
    private RollbarResponseHandler responseHandler;
    private PayloadFilter filter;
    private PayloadTransform transform;
    private Sender sender;

    public Rollbar(String accessToken, String environment) {
        this(accessToken, environment, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Rollbar(String accessToken, String environment, String codeVersion, String platform, String language, String framework, String context, Request request, Person person, Server server, Map<String, Object> custom, String fingerprint, String title, UUID uuid, Notifier notifier, RollbarResponseHandler responseHandler, PayloadFilter filter, PayloadTransform transform, Sender sender) {
        Validate.isNotNullOrWhitespace(accessToken, "accessToken");
        Validate.isNotNullOrWhitespace(environment, "environment");
        this.accessToken = accessToken;
        this.environment = environment;
        this.codeVersion = codeVersion;
        this.platform = platform;
        this.language = language;
        this.framework = framework;
        this.context = context;
        this.request = request;
        this.person = person;
        this.server = server;
        this.custom = new LinkedHashMap<String, Object>(custom);
        this.fingerprint = fingerprint;
        this.title = title;
        this.uuid = uuid;
        this.notifier = notifier;
        this.responseHandler = responseHandler;
        this.filter = filter;
        this.transform = transform;
        this.sender = sender;
    }

    public void handleUncaughtErrors() {
        handleUncaughtErrors(Thread.currentThread());
    }

    public void handleUncaughtErrors(Thread thread) {
        Validate.isNotNull(thread, "thread");
        final Rollbar rollbar = this;
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                rollbar.log(e);
            }
        });
    }

    public void critical(Throwable error) {
        log(error, null, null, Level.CRITICAL);
    }

    public void error(Throwable error) {
        log(error, null, null, Level.ERROR);
    }

    public void warning(Throwable error) {
        log(error, null, null, Level.WARNING);
    }

    public void info(Throwable error) {
        log(error, null, null, Level.INFO);
    }

    public void debug(Throwable error) {
        log(error, null, null, Level.DEBUG);
    }

    public void log(Throwable error) {
        log(error, null, null, null);
    }

    public void log(Throwable error, Level level) {
        log(error, null, null, level);
    }

    public void critical(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.CRITICAL);
    }

    public void error(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.ERROR);
    }

    public void warning(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.WARNING);
    }

    public void info(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.INFO);
    }

    public void debug(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.DEBUG);
    }

    public void log(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, null);
    }

    public void log(Throwable error, Map<String, Object> custom, Level level) {
        log(error, custom, null, level);
    }

    public void critical(Throwable error, String description) {
        log(error, null, description, Level.CRITICAL);
    }

    public void error(Throwable error, String description) {
        log(error, null, description, Level.ERROR);
    }

    public void warning(Throwable error, String description) {
        log(error, null, description, Level.WARNING);
    }

    public void info(Throwable error, String description) {
        log(error, null, description, Level.INFO);
    }

    public void debug(Throwable error, String description) {
        log(error, null, description, Level.DEBUG);
    }

    public void log(Throwable error, String description) {
        log(error, null, description, null);
    }

    public void log(Throwable error, String description, Level level) {
        log(error, null, description, level);
    }

    public void critical(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.CRITICAL);
    }

    public void error(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.ERROR);
    }

    public void warning(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.WARNING);
    }

    public void info(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.INFO);
    }

    public void debug(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.DEBUG);
    }

    public void log(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, null);
    }

    public void critical(String message) {
        log(null, null, message, Level.CRITICAL);
    }

    public void error(String message) {
        log(null, null, message, Level.ERROR);
    }

    public void warning(String message) {
        log(null, null, message, Level.WARNING);
    }

    public void info(String message) {
        log(null, null, message, Level.INFO);
    }

    public void debug(String message) {
        log(null, null, message, Level.DEBUG);
    }

    public void log(String message) {
        log(null, null, message, null);
    }

    public void log(String message, Level level) {
        log(null, null, message, level);
    }

    public void critical(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.CRITICAL);
    }

    public void error(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.ERROR);
    }

    public void warning(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.WARNING);
    }

    public void info(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.INFO);
    }

    public void debug(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.DEBUG);
    }

    public void log(String message, Map<String, Object> custom) {
        log(null, custom, message, null);
    }

    public void log(String message, Map<String, Object> custom, Level level) {
        log(null, custom, message, level);
    }

    public void log(Throwable error, Map<String, Object> custom, String description, Level level) {
        Payload p = buildPayload(error, custom, description, level);
        sendPayload(p, error, description);
    }

    private Payload buildPayload(Throwable error, Map<String, Object> custom, String description, Level level) {
        Body body;
        if (error == null) {
            body = Body.fromString(description, custom);
            custom = null;
        } else {
            body = Body.fromError(error);
        }

        level = level == null ? level(error, description) : level;

        Map<String, Object> defaultCustom = getCustom();
        if (defaultCustom != null || custom != null) {
            Map<String, Object> finalCustom = new LinkedHashMap<String, Object>();
            if (defaultCustom != null) {
                finalCustom.putAll(defaultCustom);
            }
            if (custom != null) {
                finalCustom.putAll(custom);
            }
            custom = finalCustom;
        }

        Data data = new Data(getEnvironment(), body, level, Instant.now(),
          getCodeVersion(), getPlatform(), getLanguage(), getFramework(),
          getContext(), getRequest(), getPerson(), getServer(), custom,
          getFingerprint(), getTitle(), getUuid(), getNotifier());
        Payload p = new Payload(getAccessToken(), data);

        if (transform != null) {
            return transform.transform(p, error, description);
        }

        return p;
    }

    private void sendPayload(Payload p, Throwable error, String description) {
        if (filter == null || filter.shouldSend(p, error, description)) {
            sender.send(p, responseHandler);
        }
    }

    public Level level(Throwable error, String description) {
        if (error == null) {
            return Level.WARNING;
        }
        if (error instanceof Error) {
            return Level.CRITICAL;
        }
        return Level.ERROR;
    }

    public String getAccessToken() {
        return accessToken;
    }

    protected void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public Rollbar accessToken(String accessToken) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getEnvironment() {
        return environment;
    }

    protected void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Rollbar environment(String environment) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getCodeVersion() {
        return codeVersion;
    }

    protected void setCodeVersion(String codeVersion) {
        this.codeVersion = codeVersion;
    }

    public Rollbar codeVersion(String codeVersion) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getPlatform() {
        return platform;
    }

    protected void setPlatform(String platform) {
        this.platform = platform;
    }

    public Rollbar platform(String platform) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getLanguage() {
        return language;
    }

    protected void setLanguage(String language) {
        this.language = language;
    }

    public Rollbar language(String language) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getFramework() {
        return framework;
    }

    protected void setFramework(String framework) {
        this.framework = framework;
    }

    public Rollbar framework(String framework) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getContext() {
        return context;
    }

    protected void setContext(String context) {
        this.context = context;
    }

    public Rollbar context(String context) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public Request getRequest() {
        return request;
    }

    protected void setRequest(Request request) {
        this.request = request;
    }

    public Rollbar request(Request request) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public Person getPerson() {
        return person;
    }

    protected void setPerson(Person person) {
        this.person = person;
    }

    public Rollbar person(Person person) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public Server getServer() {
        return server;
    }

    protected void setServer(Server server) {
        this.server = server;
    }

    public Rollbar server(Server server) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public Map<String, Object> getCustom() {
        return custom;
    }

    protected void setCustom(Map<String, Object> custom) {
        this.custom = custom;
    }

    public Rollbar custom(Map<String, Object> custom) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getFingerprint() {
        return fingerprint;
    }

    protected void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public Rollbar fingerprint(String fingerprint) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public Rollbar title(String title) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public UUID getUuid() {
        return uuid;
    }

    protected void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Rollbar uuid(UUID uuid) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public Notifier getNotifier() {
        return notifier;
    }

    protected void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }

    public Rollbar notifier(Notifier notifier) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public RollbarResponseHandler getResponseHandler() {
        return responseHandler;
    }

    protected void setResponseHandler(RollbarResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    public Rollbar responseHandler(RollbarResponseHandler responseHandler) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public PayloadFilter getFilter() {
        return filter;
    }

    protected void setFilter(PayloadFilter filter) {
        this.filter = filter;
    }

    public Rollbar filter(PayloadFilter filter) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public PayloadTransform getTransform() {
        return transform;
    }

    protected void setTransform(PayloadTransform transform) {
        this.transform = transform;
    }

    public Rollbar transform(PayloadTransform transform) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }

    public Sender getSender() {
      return sender;
    }

    protected void setSender(Sender sender) {
        this.sender = sender;
    }

    public Rollbar sender(Sender sender) {
        return new Rollbar(accessToken, environment, codeVersion, platform, language, framework, context, request, person, server, custom, fingerprint, title, uuid, notifier, responseHandler, filter, transform, sender);
    }
}
