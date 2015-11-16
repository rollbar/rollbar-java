package com.rollbar.payload.data.body;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Extensible;
import com.rollbar.payload.utilities.Validate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Message extends Extensible implements BodyContents {
    public static final String BODY_KEY = "body";

    @Override
    protected Set<String> getKnownMembers() {
        Set<String> result = new HashSet<String>(4);
        result.add(BODY_KEY);
        return result;
    }

    public Message(String body) throws ArgumentNullException {
        this(body, new HashMap<String, Object>());
    }

    public Message(String body, HashMap<String, Object> members) throws ArgumentNullException {
        super(members);
        Validate.isNotNullOrWhitespace(body, "body");
        put(BODY_KEY, body);
    }

    public String body() {
        return (String) get(BODY_KEY);
    }

    public Message body(String body) throws ArgumentNullException {
        return new Message(body, members);
    }
}
