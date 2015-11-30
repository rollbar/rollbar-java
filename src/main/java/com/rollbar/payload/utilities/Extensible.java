package com.rollbar.payload.utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for classes that can be extended with arbitrary metadata (as per the
 * <a href='https://rollbar.com/docs/api/items_post/'>Rollbar spec</a>).
 * This class, unlike the rest of the classes is mutable. Extra caution is therefore warranted.
 * @param <T> The extensible type itself.
 */
public abstract class Extensible<T extends Extensible<T>> {
    /**
     * Constructor
     * @param members the HashMap of all members already in this object
     */
    protected Extensible(HashMap<String, Object> members) {
        this.members = members == null ? new HashMap<String, Object>() : new HashMap<String, Object>(members);
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
     * The HashMap containing all members.
     */
    protected final HashMap<String, Object> members;

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
        T returnVal = this.copy();
        returnVal.members.put(name, value);
        return returnVal;
    }

    /**
     * Get the keys in this extensible
     * @param withoutKnownMembers true if you want to leave out known members
     * @return the keys
     */
    public Set<String> keys(boolean withoutKnownMembers) {
        Set<String> keys = new HashSet<String>(members.keySet());
        if (withoutKnownMembers) {
            keys.removeAll(knownMembers());
        }
        return keys;
    }

    /**
     * Get a copy of the members.
     * @return a copy of the members in this Extensible.
     */
    public HashMap<String, Object> getMembers() {
        return new HashMap<String, Object>(members);
    }
}
