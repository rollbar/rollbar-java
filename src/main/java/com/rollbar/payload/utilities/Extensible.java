package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

import java.util.HashMap;
import java.util.Set;

public abstract class Extensible {
    protected Extensible(HashMap<String, Object> members) {
        this.members = members == null ? new HashMap<String, Object>() : new HashMap<String, Object>(members);
    }

    protected abstract Set<String> getKnownMembers();

    private Set<String> knownMembers;

    private boolean isKnownMember(String name) {
        if (knownMembers == null) {
            knownMembers = getKnownMembers();
        }
        return knownMembers.contains(name);
    }

    protected HashMap<String, Object> members;

    public Object get(String name) {
        return members.getOrDefault(name, null);
    }

    public Extensible put(String name, Object value) {
        if (isKnownMember(name)) {
            final String msgFmt = "'%s' is a known member and must be set with the corresponding method";
            throw new IllegalArgumentException(String.format(msgFmt, name));
        }
        members.put(name, value);
        return this;
    }

    public Iterable<String> keys() {
        return members.keySet();
    }

    JsonElement asJsonElement(JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(members);
    }
}
