package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.JsonSerializable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the context around the code where the error occurred (lines before, 'pre', and after, 'post')
 */
public class CodeContext implements JsonSerializable {
    private final String[] pre;
    private final String[] post;

    /**
     * Constructor
     */
    public CodeContext() {
        this(null, null);
    }

    /**
     * Constructor
     * @param pre the lines of code before the one that triggered the error
     * @param post the lines of code after the one that triggered the error
     */
    public CodeContext(String[] pre, String[] post) {
        this.pre = pre == null ? null : pre.clone();
        this.post = post == null ? null : post.clone();
    }

    /**
     * @return the lines of code before the one that triggered the error
     */
    public String[] pre() {
        return pre == null ? null : pre.clone();
    }

    /**
     * Set the lines of code before the one that triggered the error in a copy of this CodeContext
     * @param pre the new `pre` lines of code
     * @return a copy of this CodeContext with pre overridden
     */
    public CodeContext pre(String[] pre) {
        return new CodeContext(pre, post);
    }

    /**
     * @return the lines of code after the one that triggered the error
     */
    public String[] post() {
        return post == null ? null : post.clone();
    }

    /**
     * Set the lines of code after the one that triggered the error in a copy of this CodeContext
     * @param post the new `post` lines of code
     * @return a copy of this CodeContext with post overridden
     */
    public CodeContext post(String[] post) {
        return new CodeContext(pre, post);
    }

    public Map<String, Object> asJson() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("pre", pre());
        obj.put("post", post());
        return obj;
    }
}
