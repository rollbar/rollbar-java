package com.rollbar.payload.data.body;

import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.Extensible;
import com.rollbar.utilities.Validate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a message (text) sent to Rollbar, possible with additional information
 */
public class Message extends Extensible<Message> implements BodyContents {
    public static final String BODY_KEY = "body";

    private Message(Map<String, Object> members) {
        super(members);
    }

    /**
     * Constructor
     * @param body not nullable, a string to send to Rollbar
     * @throws ArgumentNullException if body is null
     */
    public Message(String body) throws ArgumentNullException {
        this(body, null);
    }

    /**
     * Constructor
     * @param body not nullable, a string to send to Rollbar
     * @param members additional information to send with the message
     * @throws ArgumentNullException if body is null
     */
    public Message(String body, Map<String, Object> members) throws ArgumentNullException {
        super(members);
        Validate.isNotNullOrWhitespace(body, "body");
        putKnown(BODY_KEY, body);
    }

    @Override
    protected Set<String> getKnownMembers() {
        Set<String> result = new HashSet<String>(4);
        result.add(BODY_KEY);
        return result;
    }

    @Override
    public Message copy() {
        return new Message(getMembers());
    }

    /**
     * @return the text of the message
     */
    public String body() {
        return (String) get(BODY_KEY);
    }

    /**
     * Set body in a copy of this message
     * @param body the new body
     * @return a copy of this message with body overridden
     * @throws ArgumentNullException if body is null
     */
    public Message body(String body) throws ArgumentNullException {
        return new Message(body, getMembers());
    }

    public String getKeyName() {
        return "message";
    }
}
