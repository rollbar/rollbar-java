package com.rollbar.payload.data;

import com.rollbar.payload.utilities.Extensible;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Request extends Extensible<Request> {
    public static final String URL_KEY = "url";
    public static final String METHOD_KEY = "method";
    public static final String HEADERS_KEY = "headers";
    public static final String PARAMS_KEY = "params";
    public static final String GET_KEY = "GET";
    public static final String QUERY_STRING_KEY = "query_string";
    public static final String POST_KEY = "POST";
    public static final String BODY_KEY = "body";
    public static final String USER_IP_KEY = "user_ip";
    private static final Set<String> keys = new HashSet<String>();

    static {
        final String[] keys = new String[]{
                URL_KEY,
                METHOD_KEY,
                HEADERS_KEY,
                PARAMS_KEY,
                GET_KEY,
                QUERY_STRING_KEY,
                POST_KEY,
                BODY_KEY,
                USER_IP_KEY
        };
        Collections.addAll(Request.keys, keys);
    }

    @Override
    protected Set<String> getKnownMembers() {
        return keys;
    }

    @Override
    public Request copy() {
        return new Request(members);
    }

    private Request(HashMap<String, Object> members) {
        super(members);
    }

    public Request() {
        this(null, null, null, null, null, null, null, null, null, null);
    }

    public Request(String url, String method, HashMap<String, String> headers, HashMap<String, String> params,
                   HashMap<String, String> get, String queryString, HashMap<String, Object> post, String body,
                   InetAddress userIp) {
        this(url, method, headers, params, get, queryString, post, body, userIp, new HashMap<String, Object>());
    }

    public Request(String url, String method, HashMap<String, String> headers, HashMap<String, String> params,
                   HashMap<String, String> get, String queryString, HashMap<String, Object> post, String body,
                   InetAddress userIp, HashMap<String, Object> members) {
        super(members);
        this.members.put(URL_KEY, url);
        this.members.put(METHOD_KEY, method);
        this.members.put(HEADERS_KEY, headers == null ? null : new HashMap<String, String>(headers));
        this.members.put(PARAMS_KEY, params == null ? null : new HashMap<String, String>(params));
        this.members.put(GET_KEY, get == null ? null : new HashMap<String, String>(get));
        this.members.put(QUERY_STRING_KEY, queryString);
        this.members.put(POST_KEY, post == null ? null : new HashMap<String, Object>(post));
        this.members.put(BODY_KEY, body);
        this.members.put(USER_IP_KEY, userIp == null ? null : userIp.getHostAddress());
    }

    public String url() {
        return (String) get(URL_KEY);
    }

    public Request url(String url) {
        return new Request(url, method(), headers(), params(), getGet(), queryString(), post(), body(), userIp(), members);
    }

    public String method() {
        return (String) get(METHOD_KEY);
    }

    public Request method(String method) {
        return new Request(url(), method, headers(), params(), getGet(), queryString(), post(), body(), userIp(), members);
    }

    public HashMap<String, String> headers() {
        @SuppressWarnings("unchecked")
        HashMap<String, String> headers = (HashMap<String, String>) get(HEADERS_KEY);
        return headers;
    }

    public Request headers(HashMap<String, String> headers) {
        return new Request(url(), method(), headers, params(), getGet(), queryString(), post(), body(), userIp(), members);
    }

    public HashMap<String, String> params() {
        @SuppressWarnings("unchecked")
        HashMap<String, String> params = (HashMap<String, String>) get(PARAMS_KEY);
        return params;
    }

    public Request params(HashMap<String, String> params) {
        return new Request(url(), method(), headers(), params, getGet(), queryString(), post(), body(), userIp(), members);
    }

    public HashMap<String, String> getGet() {
        @SuppressWarnings("unchecked")
        HashMap<String, String> get = (HashMap<String, String>) get(GET_KEY);
        return get;
    }

    public Request setGet(HashMap<String, String> get) {
        return new Request(url(), method(), headers(), params(), get, queryString(), post(), body(), userIp(), members);
    }

    public String queryString() {
        return (String) get(QUERY_STRING_KEY);
    }

    public Request queryString(String queryString) {
        return new Request(url(), method(), headers(), params(), getGet(), queryString, post(), body(), userIp(), members);
    }

    public HashMap<String, Object> post() {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> post = (HashMap<String, Object>) get(POST_KEY);
        return post;
    }

    public Request post(HashMap<String, Object> post) {
        return new Request(url(), method(), headers(), params(), getGet(), queryString(), post, body(), userIp(), members);
    }

    public String body() {
        return (String) get(BODY_KEY);
    }

    public Request body(String body) {
        return new Request(url(), method(), headers(), params(), getGet(), queryString(), post(), body, userIp(), members);
    }

    public InetAddress userIp() {
        Object ip = get(USER_IP_KEY);
        try {
            return ip == null ? null : InetAddress.getByName((String) get(USER_IP_KEY));
        }
        catch (java.net.UnknownHostException e) {
            return null;
        }
    }

    public Request userIp(InetAddress userIp) {
        return new Request(url(), method(), headers(), params(), getGet(), queryString(), post(), body(), userIp, members);
    }
}
