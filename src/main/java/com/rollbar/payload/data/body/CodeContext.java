package com.rollbar.payload.data.body;

/**
 * Created by chris on 11/13/15.
 */
public class CodeContext {
    private final String[] pre;
    private final String[] post;

    public CodeContext() {
        this(null, null);
    }

    public CodeContext(String[] pre, String[] post) {
        this.pre = pre == null ? null : pre.clone();
        this.post = post == null ? null : post.clone();
    }

    public String[] pre() {
        return pre == null ? null : pre.clone();
    }

    public CodeContext pre(String[] pre) {
        return new CodeContext(pre, post);
    }

    public String[] post() {
        return post == null ? null : post.clone();
    }

    public CodeContext post(String[] post) {
        return new CodeContext(pre, post);
    }
}
