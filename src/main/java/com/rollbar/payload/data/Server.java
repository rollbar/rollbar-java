package com.rollbar.payload.data;

import com.rollbar.payload.utilities.Extensible;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
        return new Server(members);
    }

    private Server(HashMap<String, Object> members) {
        super(members);
    }

    public Server() {
        this(null, null, null, null, null);
    }

    public Server(String host, String root, String branch, String codeVersion) {
        this(host, root, branch, codeVersion, new HashMap<String, Object>());
    }

    public Server(String host, String root, String branch, String codeVersion, HashMap<String, Object> members) {
        super(members);
        this.members.put(HOST_KEY, host);
        this.members.put(ROOT_KEY, root);
        this.members.put(BRANCH_KEY, branch);
        this.members.put(CODE_VERSION_KEY, codeVersion);
    }

    public String host() {
        return (String) get(HOST_KEY);
    }

    public Server host(String host) {
        return new Server(host, root(), branch(), codeVersion(), members);
    }

    public String root() {
        return (String) get(ROOT_KEY);
    }

    public Server root(String root) {
        return new Server(host(), root, branch(), codeVersion(), members);
    }

    public String branch() {
        return (String) get(BRANCH_KEY);
    }

    public Server branch(String branch) {
        return new Server(host(), root(), branch, codeVersion(), members);
    }

    public String codeVersion() {
        return (String) get(CODE_VERSION_KEY);
    }

    public Server codeVersion(String codeVersion) {
        return new Server(host(), root(), branch(), codeVersion, members);
    }
}
