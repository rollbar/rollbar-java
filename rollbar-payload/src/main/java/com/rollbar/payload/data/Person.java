package com.rollbar.payload.data;

import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.InvalidLengthException;
import com.rollbar.utilities.JsonSerializable;
import com.rollbar.utilities.Validate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the user affected by an error
 */
public class Person implements JsonSerializable {
    private final String id;
    private final String username;
    private final String email;

    /**
     * Constructor
     * @param id the affected user's id
     * @throws ArgumentNullException if {@code id} is null
     */
    public Person(String id) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(id, "id");
        this.id = id;
        username = null;
        email = null;
    }

    /**
     * Constructor
     * @param id the affected user's id
     * @param username the affected user's username
     * @param email the affected user's email address
     * @throws InvalidLengthException if {@code username} or {@code email} are longer than 255 characters
     * @throws ArgumentNullException if {@code id} is null
     */
    public Person(String id, String username, String email) throws InvalidLengthException, ArgumentNullException {
        Validate.isNotNullOrWhitespace(id, "id");
        this.id = id;
        if (username != null) {
            Validate.maxLength(username, 255, "username");
        }
        this.username = username;
        if (email != null) {
            Validate.maxLength(email, 255, "email");
        }
        this.email = email;
    }

    /**
     * @return the affected user's id
     */
    public String id() {
        return this.id;
    }

    /**
     * Set the id on a copy of this Person
     * @param id the new id
     * @return a copy of this person with the new id
     * @throws ArgumentNullException if {@code id} is null
     */
    public Person id(String id) throws ArgumentNullException {
        return new Person(id, username, email);
    }

    /**
     * @return the affected user's username
     */
    public String username() {
        return this.username;
    }

    /**
     * Set the username on a copy of this Person
     * @param username the new username
     * @return a copy of this person with the new username
     * @throws InvalidLengthException if {@code username} is longer than 255 characters
     */
    public Person username(String username) throws InvalidLengthException {
        return new Person(id, username, email);
    }

    /**
     * @return the affected user's email
     */
    public String email() {
        return this.email;
    }

    /**
     * Set the email on a copy of this Person
     * @param email the new email
     * @return a copy of this person with the new email
     * @throws InvalidLengthException if {@code email} is longer than 255 characters
     */
    public Person email(String email) throws InvalidLengthException {
        return new Person(id, username, email);
    }

    public Map<String, Object> asJson() {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("id", id());

        if (username != null) obj.put("username", username());
        if (email != null) obj.put("email", email());
        return obj;
    }
}
