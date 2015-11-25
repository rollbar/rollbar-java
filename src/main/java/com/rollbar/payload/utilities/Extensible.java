package com.rollbar.payload.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class Extensible<T extends Extensible<T>> {
    protected Extensible(HashMap<String, Object> members) {
        this.members = members == null ? new HashMap<String, Object>() : new HashMap<String, Object>(members);
    }

    protected abstract Set<String> getKnownMembers();

    private Set<String> knownMembers;

    private Set<String> knownMembers() {
        if (knownMembers == null) {
            knownMembers = getKnownMembers();
        }
        return knownMembers;
    }

    private boolean isKnownMember(String name) {
        return knownMembers().contains(name);
    }

    protected final HashMap<String, Object> members;

    public Object get(String name) {
        return members.getOrDefault(name, null);
    }

    public abstract T copy();

    public T put(String name, Object value) {
        if (isKnownMember(name)) {
            final String msgFmt = "'%s' is a known member and must be set with the corresponding method";
            throw new IllegalArgumentException(String.format(msgFmt, name));
        }
        T returnVal = (T) this.copy(); // Safe because Extensible<T extends Extensible<T>>
        returnVal.members.put(name, value);
        return returnVal;
    }

    public Set<String> keys(boolean withoutKnownMembers) {
        Set<String> keys = new HashSet<String>(members.keySet());
        if (withoutKnownMembers) {
            keys.removeAll(knownMembers());
        }
        return keys;
    }

    JsonElement asJsonElement(JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(members);
    }
}
