package com.rollbar;

import com.rollbar.http.*;
import com.rollbar.payload.Payload;
import com.rollbar.payload.data.*;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The notifier itself. Anything that can be statically set should be set by passed in through the constructor.
 * Anything that needs to be dynamically determined should be configured by subclassing and overriding the `getXXX`
 * methods.
 * There are
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
    private Notifier notifier;
    private RollbarResponseHandler responseHandler;
    private PayloadFilter filter;
    private PayloadTransform transform;
    private Sender sender;

    /**
     * Construct a notifier defaults for everything including Sender.
     * Caution: default sender is slow and blocking. Consider providing a Sender overload.
     * @param accessToken not nullable, the access token to send payloads to
     * @param environment not nullable, the environment to send payloads under
     */
    public Rollbar(String accessToken, String environment) {
        this(accessToken, environment, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Construct notifier, defaults for everything but Sender.
     * @param accessToken not nullable, the access token to send payloads to
     * @param environment not nullable, the environment to send payloads under
     * @param sender the sender to use. If null uses default: {@link PayloadSender}
     */
    public Rollbar(String accessToken, String environment, Sender sender) {
        this(accessToken, environment, sender, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Construct notifier with static values for all configuration options set. Anything left null will use the default
     * value. If appropriate.
     * @param accessToken not nullable, the access token to send payloads to
     * @param environment not nullable, the environment to send payloads under
     * @param sender the sender to use. If null uses default: {@link PayloadSender}
     * @param codeVersion the version of the code currently running. If code checked out on server: `git rev-parse HEAD`
     * @param platform the platform you're running. (JVM version, or similar).
     * @param language the main language you're running ("java" by default, override w/ "clojure", "scala" etc.).
     * @param framework the framework you're using ("Play", "Spring", etc.).
     * @param context a mnemonic for finding the code responsible (e.g. controller name, module name)
     * @param request the HTTP request that triggered this error. Can be set if the IOC container can work per-request.
     * @param person the affected person. Can be set if the IOC container can work per-request.
     * @param server info about this server. This can be statically set.
     * @param custom custom info to send with *every* error. Can be dynamically or statically set.
     * @param notifier information about this notifier. Default {@code new Notifier()} ({@link Notifier}.
     * @param responseHandler what to do with the response. Use this to check for failures and handle some other way.
     * @param filter filter used to determine if you will send payload. Receives *transformed* payload.
     * @param transform alter payload before sending.
     */
    public Rollbar(String accessToken, String environment, Sender sender, String codeVersion, String platform,
                   String language, String framework, String context, Request request, Person person, Server server,
                   Map<String, Object> custom, Notifier notifier, RollbarResponseHandler responseHandler,
                   PayloadFilter filter, PayloadTransform transform) {
        Validate.isNotNullOrWhitespace(accessToken, "accessToken");
        Validate.isNotNullOrWhitespace(environment, "environment");
        this.sender = sender == null ? new PayloadSender() : sender;
        this.accessToken = accessToken;
        this.environment = environment;
        this.codeVersion = codeVersion;
        this.platform = platform;
        this.language = language;
        this.framework = framework == null ? "java" : framework;
        this.context = context;
        this.request = request;
        this.person = person;
        this.server = server;
        this.custom = new LinkedHashMap<String, Object>(custom);
        this.notifier = notifier == null ? new Notifier() : notifier;
        this.responseHandler = responseHandler;
        this.filter = filter;
        this.transform = transform;
    }

    /**
     * Handle all uncaught errors on current thread with this `Rollbar`
     */
    public void handleUncaughtErrors() {
        handleUncaughtErrors(Thread.currentThread());
    }

    /**
     * Handle all uncaught errors on {@code thread} with this `Rollbar`
     * @param thread the thread to handle errors on
     */
    public void handleUncaughtErrors(Thread thread) {
        Validate.isNotNull(thread, "thread");
        final Rollbar rollbar = this;
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                rollbar.log(e);
            }
        });
    }

    /**
     * Record a critical error
     * @param error the error
     */
    public void critical(Throwable error) {
        log(error, null, null, Level.CRITICAL);
    }

    /**
     * Record an error
     * @param error the error
     */
    public void error(Throwable error) {
        log(error, null, null, Level.ERROR);
    }

    /**
     * Record an error as a warning
     * @param error the error
     */
    public void warning(Throwable error) {
        log(error, null, null, Level.WARNING);
    }

    /**
     * Record an error as an info
     * @param error the error
     */
    public void info(Throwable error) {
        log(error, null, null, Level.INFO);
    }

    /**
     * Record an error as debugging information
     * @param error the error
     */
    public void debug(Throwable error) {
        log(error, null, null, Level.DEBUG);
    }

    /**
     * Log an error at the level returned by {@link Rollbar#level}
     * @param error the error
     */
    public void log(Throwable error) {
        log(error, null, null, null);
    }

    /**
     * Log an error at level specified.
     * @param error the error
     * @param level the level of the error
     */
    public void log(Throwable error, Level level) {
        log(error, null, null, level);
    }

    /**
     * Record a critical error with extra information attached
     * @param error the error
     * @param custom the extra information
     */
    public void critical(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.CRITICAL);
    }

    /**
     * Record an error with extra information attached
     * @param error the error
     * @param custom the extra information
     */
    public void error(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.ERROR);
    }

    /**
     * Record a warning error with extra information attached
     * @param error the error
     * @param custom the extra information
     */
    public void warning(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.WARNING);
    }

    /**
     * Record an info error with extra information attached
     * @param error the error
     * @param custom the extra information
     */
    public void info(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.INFO);
    }

    /**
     * Record a debug error with extra information attached
     * @param error the error
     * @param custom the extra information
     */
    public void debug(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, Level.DEBUG);
    }

    /**
     * Record an error with extra information attached at the default level retured by {@link Rollbar#level}
     * @param error the error
     * @param custom the extra information
     */
    public void log(Throwable error, Map<String, Object> custom) {
        log(error, custom, null, null);
    }

    /**
     * Record an error with extra information attached at the level specified
     * @param error the error
     * @param custom the extra information
     * @param level the level
     */
    public void log(Throwable error, Map<String, Object> custom, Level level) {
        log(error, custom, null, level);
    }

    /**
     * Record a critical error with human readable description
     * @param error the error
     * @param description human readable description of error
     */
    public void critical(Throwable error, String description) {
        log(error, null, description, Level.CRITICAL);
    }

    /**
     * Record an error with human readable description
     * @param error the error
     * @param description human readable description of error
     */
    public void error(Throwable error, String description) {
        log(error, null, description, Level.ERROR);
    }

    /**
     * Record a warning with human readable description
     * @param error the error
     * @param description human readable description of error
     */
    public void warning(Throwable error, String description) {
        log(error, null, description, Level.WARNING);
    }

    /**
     * Record an info error with human readable description
     * @param error the error
     * @param description human readable description of error
     */
    public void info(Throwable error, String description) {
        log(error, null, description, Level.INFO);
    }

    /**
     * Record a debug error with human readable description
     * @param error the error
     * @param description human readable description of error
     */
    public void debug(Throwable error, String description) {
        log(error, null, description, Level.DEBUG);
    }

    /**
     * Record an error with human readable description at the default level returned by {@link Rollbar#level}
     * @param error the error
     * @param description human readable description of error
     */
    public void log(Throwable error, String description) {
        log(error, null, description, null);
    }

    /**
     * Record a debug error with human readable description at the specified level
     * @param error the error
     * @param description human readable description of error
     * @param level the level
     */
    public void log(Throwable error, String description, Level level) {
        log(error, null, description, level);
    }

    /**
     * Record a critical error with custom parameters and human readable description
     * @param error the error
     * @param custom the custom data
     * @param description the human readable description of error
     */
    public void critical(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.CRITICAL);
    }

    /**
     * Record an error with custom parameters and human readable description
     * @param error the error
     * @param custom the custom data
     * @param description the human readable description of error
     */
    public void error(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.ERROR);
    }

    /**
     * Record a warning error with custom parameters and human readable description
     * @param error the error
     * @param custom the custom data
     * @param description the human readable description of error
     */
    public void warning(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.WARNING);
    }

    /**
     * Record an info error with custom parameters and human readable description
     * @param error the error
     * @param custom the custom data
     * @param description the human readable description of error
     */
    public void info(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.INFO);
    }

    /**
     * Record a debug error with custom parameters and human readable description
     * @param error the error
     * @param custom the custom data
     * @param description the human readable description of error
     */
    public void debug(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, Level.DEBUG);
    }

    /**
     * Record an error with custom parameters and human readable description at the default level returned by
     * {@link Rollbar#level}
     * @param error the error
     * @param custom the custom data
     * @param description the human readable description of error
     */
    public void log(Throwable error, Map<String, Object> custom, String description) {
        log(error, custom, description, null);
    }

    /**
     * Record a critical message
     * @param message the message
     */
    public void critical(String message) {
        log(null, null, message, Level.CRITICAL);
    }

    /**
     * Record an error message
     * @param message the message
     */
    public void error(String message) {
        log(null, null, message, Level.ERROR);
    }

    /**
     * Record a warning message
     * @param message the message
     */
    public void warning(String message) {
        log(null, null, message, Level.WARNING);
    }

    /**
     * Record an informational message
     * @param message the message
     */
    public void info(String message) {
        log(null, null, message, Level.INFO);
    }

    /**
     * Record a debugging message
     * @param message the message
     */
    public void debug(String message) {
        log(null, null, message, Level.DEBUG);
    }

    /**
     * Record a debugging message at the level returned by {@link Rollbar#level} (WARNING unless level is overriden)
     * @param message the message
     */
    public void log(String message) {
        log(null, null, message, null);
    }

    /**
     * Record a message at the level specified
     * @param message the message
     * @param level the level
     */
    public void log(String message, Level level) {
        log(null, null, message, level);
    }

    /**
     * Record a critical message with extra information attached
     * @param message the message
     * @param custom the extra information
     */
    public void critical(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.CRITICAL);
    }

    /**
     * Record a error message with extra information attached
     * @param message the message
     * @param custom the extra information
     */
    public void error(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.ERROR);
    }

    /**
     * Record a warning message with extra information attached
     * @param message the message
     * @param custom the extra information
     */
    public void warning(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.WARNING);
    }

    /**
     * Record an informational message with extra information attached
     * @param message the message
     * @param custom the extra information
     */
    public void info(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.INFO);
    }

    /**
     * Record a debugging message with extra information attached
     * @param message the message
     * @param custom the extra information
     */
    public void debug(String message, Map<String, Object> custom) {
        log(null, custom, message, Level.DEBUG);
    }

    /**
     * Record a message with extra information attached at the default level returned by {@link Rollbar#level}, (WARNING
     * unless level overriden).
     * @param message the message
     * @param custom the extra information
     */
    public void log(String message, Map<String, Object> custom) {
        log(null, custom, message, null);
    }

    /**
     * Record a message with extra infomation attached at the specified level
     * @param message the message
     * @param custom the extra information
     * @param level the level
     */
    public void log(String message, Map<String, Object> custom, Level level) {
        log(null, custom, message, level);
    }

    /**
     * Record an error or message with extra data at the level specified. At least ene of `error` or `description` must
     * be non-null. If error is null, `description` will be sent as a message. If error is non-null, description will be
     * sent as the description of the error.
     * Custom data will be attached to message if the error is null. Custom data will extend whatever
     * {@link Rollbar#custom} returns.
     * @param error the error (if any)
     * @param custom the custom data (if any)
     * @param description the description of the error, or the message to send
     * @param level the level to send it at
     */
    public void log(Throwable error, Map<String, Object> custom, String description, Level level) {
        Payload p = buildPayload(error, custom, description, level);
        sendPayload(p, error, description);
    }

    private Payload buildPayload(Throwable error, Map<String, Object> custom, String description, Level level) {
        Body body;
        if (error != null) {
            body = Body.fromError(error, description);
        } else if (description != null) {
            body = Body.fromString(description, custom);
            custom = null;
        } else {
            throw new ArgumentNullException("error | description");
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

        Data data = new Data(getEnvironment(), body, level, new Date(), getCodeVersion(), getPlatform(),
                             getLanguage(), getFramework(), getContext(), getRequest(), getPerson(), getServer(),
                             custom, null, null, null, getNotifier());
        Payload p = new Payload(getAccessToken(), data);

        if (getTransform() != null) {
            return getTransform().transform(p, error, description);
        }

        return p;
    }

    private void sendPayload(Payload p, Throwable error, String description) {
        if (getFilter() == null || getFilter().shouldSend(p, error, description)) {
            getSender().send(p, getResponseHandler());
        }
    }

    /**
     * Get the level of the error or message. By default: CRITICAL for {@link Error}, ERROR for other {@link Throwable},
     * WARNING for messages. Override to change this default.
     * @param error the error
     * @param description the description of the error, or the message if error is null
     * @return the level
     */
    public Level level(Throwable error, String description) {
        if (error == null) {
            return Level.WARNING;
        }
        if (error instanceof Error) {
            return Level.CRITICAL;
        }
        return Level.ERROR;
    }

    /**
     * @return the access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Set the accessToken
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param accessToken the new access token
     */
    protected void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Get a copy of this Rollbar with accessToken overridden
     * @param accessToken the access token
     * @return a copy of this Rollbar with access token overridden
     * @throws ArgumentNullException if environment is null
     */
    public Rollbar accessToken(String accessToken) throws ArgumentNullException {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the environment
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Set the environment
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param environment the new environment
     */
    protected void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Get a copy of this Rollbar with environment overridden
     * @param environment the new environment
     * @return a copy of this Rollbar with environment overridden
     * @throws ArgumentNullException if environment is null
     */
    public Rollbar environment(String environment) throws ArgumentNullException {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * Get the Sender used to send payloads
     * @return the Sender
     */
    public Sender getSender() {
        return sender;
    }

    /**
     * Set the sender
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param sender the new sender
     */
    protected void setSender(Sender sender) {
        this.sender = sender;
    }

    /**
     * Get a copy of this Rollbar with sender overridden
     * @param sender the new sender
     * @return a copy of this Rollbar with sender overridden
     */
    public Rollbar sender(Sender sender) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the code version
     */
    public String getCodeVersion() {
        return codeVersion;
    }

    /**
     * Set the code version
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param codeVersion the new version of the code running of the server
     */
    protected void setCodeVersion(String codeVersion) {
        this.codeVersion = codeVersion;
    }

    /**
     * Get a copy of this Rollbar with code version overridden
     * @param codeVersion the new code version
     * @return a copy of this Rollbar with code version overridden
     */
    public Rollbar codeVersion(String codeVersion) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the platform
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Set the platform
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param platform the new platform
     */
    protected void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * Get a copy of this Rollbar with platform overridden
     * @param platform the new platform
     * @return a copy of this Rollbar with platform overridden
     */
    public Rollbar platform(String platform) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Set the language
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param language the new language
     */
    protected void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Get a copy of this Rollbar with language overridden
     * @param language the new language
     * @return a copy of this Rollbar with language overridden
     */
    public Rollbar language(String language) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the framework
     */
    public String getFramework() {
        return framework;
    }

    /**
     * Set the framework
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param framework the new framework
     */
    protected void setFramework(String framework) {
        this.framework = framework;
    }

    /**
     * Get a copy of this Rollbar with framework overridden
     * @param framework the new platform
     * @return a copy of this Rollbar with framework overridden
     */
    public Rollbar framework(String framework) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the context
     */
    public String getContext() {
        return context;
    }

    /**
     * Set the context
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param context the new context
     */
    protected void setContext(String context) {
        this.context = context;
    }

    /**
     * Get a copy of this Rollbar with context overridden
     * @param context the new platform
     * @return a copy of this Rollbar with context overridden
     */
    public Rollbar context(String context) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the request
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Set the request
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param request the new request
     */
    protected void setRequest(Request request) {
        this.request = request;
    }

    /**
     * Get a copy of this Rollbar with request overridden
     * @param request the new platform
     * @return a copy of this Rollbar with request overridden
     */
    public Rollbar request(Request request) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the person
     */
    public Person getPerson() {
        return person;
    }

    /**
     * Set the person
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param person the new person
     */
    protected void setPerson(Person person) {
        this.person = person;
    }

    /**
     * Get a copy of this Rollbar with person overridden
     * @param person the new platform
     * @return a copy of this Rollbar with person overridden
     */
    public Rollbar person(Person person) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the server
     */
    public Server getServer() {
        return server;
    }

    /**
     * Set the server
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param server the new server
     */
    protected void setServer(Server server) {
        this.server = server;
    }

    /**
     * Get a copy of this Rollbar with server overridden
     * @param server the new platform
     * @return a copy of this Rollbar with server overridden
     */
    public Rollbar server(Server server) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the custom data
     */
    public Map<String, Object> getCustom() {
        return custom;
    }

    /**
     * Set the custom data
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param custom the new custom data
     */
    protected void setCustom(Map<String, Object> custom) {
        this.custom = custom;
    }

    /**
     * Get a copy of this Rollbar with custom overridden
     * @param custom the new platform
     * @return a copy of this Rollbar with custom overridden
     */
    public Rollbar custom(Map<String, Object> custom) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the notifier
     */
    public Notifier getNotifier() {
        return notifier;
    }

    /**
     * Set the notifier
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param notifier the new notifier
     */
    protected void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }

    /**
     * Get a copy of this Rollbar with notifier overridden
     * @param notifier the new platform
     * @return a copy of this Rollbar with notifier overridden
     */
    public Rollbar notifier(Notifier notifier) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the responseHandler
     */
    public RollbarResponseHandler getResponseHandler() {
        return responseHandler;
    }

    /**
     * Set the responseHandler
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param responseHandler the new responseHandler
     */
    protected void setResponseHandler(RollbarResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * Get a copy of this Rollbar with responseHandler overridden
     * @param responseHandler the new platform
     * @return a copy of this Rollbar with responseHandler overridden
     */
    public Rollbar responseHandler(RollbarResponseHandler responseHandler) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the filter
     */
    public PayloadFilter getFilter() {
        return filter;
    }

    /**
     * Set the filter
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param filter the new filter
     */
    protected void setFilter(PayloadFilter filter) {
        this.filter = filter;
    }

    /**
     * Get a copy of this Rollbar with filter overridden
     * @param filter the new platform
     * @return a copy of this Rollbar with filter overridden
     */
    public Rollbar filter(PayloadFilter filter) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }

    /**
     * @return the transform
     */
    public PayloadTransform getTransform() {
        return transform;
    }

    /**
     * Set the transform
     * Accessible for subclasses and IOC containers like spring.
     * In Subclasses DO NOT USE OUTSIDE OF CONSTRUCTOR
     * @param transform the new transform
     */
    protected void setTransform(PayloadTransform transform) {
        this.transform = transform;
    }

    /**
     * Get a copy of this Rollbar with transform overridden
     * @param transform the new platform
     * @return a copy of this Rollbar with transform overridden
     */
    public Rollbar transform(PayloadTransform transform) {
        return new Rollbar(accessToken, environment, sender, codeVersion, platform, language, framework, context, request, person, server, custom, notifier, responseHandler, filter, transform);
    }
}
