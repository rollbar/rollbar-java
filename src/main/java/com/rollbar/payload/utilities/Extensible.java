package com.rollbar.payload.utilities;

import java.util.*;

/**
 * Base class for classes that can be extended with arbitrary metadata (as per the
 * <a href='https://rollbar.com/docs/api/items_post/'>Rollbar spec</a>).
 * This class, unlike the rest of the classes is mutable. Extra caution is therefore warranted.
 * @param <T> The extensible type itself.
 */
public abstract class Extensible<T extends Extensible<T>> implements JsonSerializable {
    /**
     * Constructor
     * @param members the LinkedHashMap of all members already in this object
     */
    protected Extensible(Map<String, Object> members) {
        if (members == null) {
            this.members = new TreeMap<String, Object>();
        } else {
            this.members = new TreeMap<String, Object>(members);
        }
    }

    /**
     * Returns the 'known' keys, that are specially treated by Rollbar
     * @return the set of known keys
     */
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

    /**
     * The LinkedHashMap containing all members.
     */
    private final TreeMap<String, Object> members;

    /**
     * Get the member, or null if not present.
     * @param name the member name
     * @return null or the member at that key
     */
    public Object get(String name) {
        return members.getOrDefault(name, null);
    }

    /**
     * Copy this item. Needs to be custom per subclass. Should return the subclass itself (not an Extensible).
     * @return An exact copy of this object.
     */
    public abstract T copy();

    /**
     * Sets the member. Cannot be used to set known members.
     * @param name the member name to set.
     * @param value the value to set.
     * @return this object.
     */
    public T put(String name, Object value) throws IllegalArgumentException {
        if (isKnownMember(name)) {
            final String msgFmt = "'%s' is a known member and must be set with the corresponding method";
            throw new IllegalArgumentException(String.format(msgFmt, name));
        }
        Extensible<T> returnVal = this.copy();
        returnVal.members.put(name, value);
        @SuppressWarnings("unchecked")
        T returned = (T) returnVal;
        return returned;
    }

    /**
     * MUTATING. Use only in Constructor.
     * @param name the key
     * @param value the value
     */
    protected void putKnown(String name, Object value) {
        if (!isKnownMember(name)) {
            final String msg = "Can only set known values with this method. %s not known";
            throw new IllegalArgumentException(String.format(msg, name));
        }
        this.members.put(name, value);
    }

    /**
     * Get the keys in this extensible
     * @param withoutKnownMembers true if you want to leave out known members
     * @return the keys
     */
    public Set<String> keys(boolean withoutKnownMembers) {
        Set<String> keys = new TreeSet<String>(members.keySet());
        if (withoutKnownMembers) {
            keys.removeAll(knownMembers());
        }
        return keys;
    }

    /**
     * Get a copy of the members.
     * @return a copy of the members in this Extensible.
     */
    public Map<String, Object> getMembers() {
        return new TreeMap<String, Object>(members);
    }

    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        for(String key : knownMembers()) {
            if (this.members.containsKey(key) && this.members.getOrDefault(key, null) != null) {
                json.put(key, this.members.getOrDefault(key, null));
            }
        }
        for(Map.Entry<String, Object> entry : this.members.entrySet()) {
            if (!json.containsKey(entry.getKey())) {
                json.put(entry.getKey(), entry.getValue());
            }
        }
        return json;
    }
}
