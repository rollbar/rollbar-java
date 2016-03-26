package com.rollbar.payload.data;

import com.rollbar.payload.utilities.Extensible;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the server object sent to Rollbar
 */
public class Server extends Extensible<Server> {
    public static final String HOST_KEY = "host";
    public static final String ROOT_KEY = "root";
    public static final String BRANCH_KEY = "branch";
    public static final String CODE_VERSION_KEY = "code_version";

    @Override
    protected Set<String> getKnownMembers() {
        Set<String> result = new HashSet<String>(4);
        Collections.addAll(result, HOST_KEY, ROOT_KEY, BRANCH_KEY, CODE_VERSION_KEY);
        return result;
    }

    @Override
    public Server copy() {
        return new Server(getMembers());
    }

    private Server(Map<String, Object> members) {
        super(members);
    }

    /**
     * Constructor for an empty server
     */
    public Server() {
        this(null, null, null, null, null);
    }

    /**
     * Constructor
     * @param host the host
     * @param root the file system root
     * @param branch the current source control branch
     * @param codeVersion the current source control version (SHA, or name)
     */
    public Server(String host, String root, String branch, String codeVersion) {
        this(host, root, branch, codeVersion, null);
    }

    /**
     * Constructor
     * @param host the host
     * @param root the file system root
     * @param branch the current source control branch
     * @param codeVersion the current source control version (SHA, or name)
     * @param members the extensible members
     */
    public Server(String host, String root, String branch, String codeVersion, Map<String, Object> members) {
        super(members);
        putKnown(HOST_KEY, host);
        putKnown(ROOT_KEY, root);
        putKnown(BRANCH_KEY, branch);
        putKnown(CODE_VERSION_KEY, codeVersion);
    }

    /**
     * @return The host the code is running on
     */
    public String host() {
        return (String) get(HOST_KEY);
    }

    /**
     * Set the host the code is running on.
     * @param host the new host
     * @return the server with host overridden
     */
    public Server host(String host) {
        return new Server(host, root(), branch(), codeVersion(), getMembers());
    }

    /**
     * @return the root
     */
    public String root() {
        return (String) get(ROOT_KEY);
    }

    /**
     * Set the root
     * @param root the new host
     * @return the server with root overridden
     */
    public Server root(String root) {
        return new Server(host(), root, branch(), codeVersion(), getMembers());
    }

    /**
     * @return the branch
     */
    public String branch() {
        return (String) get(BRANCH_KEY);
    }

    /**
     * Set the branch
     * @param branch the new host
     * @return the server with branch overridden
     */
    public Server branch(String branch) {
        return new Server(host(), root(), branch, codeVersion(), getMembers());
    }

    /**
     * @return the code version
     */
    public String codeVersion() {
        return (String) get(CODE_VERSION_KEY);
    }

    /**
     * Set the code version
     * @param codeVersion the new code version
     * @return the server with code version overridden
     */
    public Server codeVersion(String codeVersion) {
        return new Server(host(), root(), branch(), codeVersion, getMembers());
    }
}
