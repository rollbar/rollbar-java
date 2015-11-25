package com.rollbar.payload.data;

import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.InvalidLengthException;
import com.rollbar.payload.utilities.Validate;

public class Person {
    private final String id;
    private final String username;
    private final String email;

    public Person(String id) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(id, "id");
        this.id = id;
        username = null;
        email = null;
    }

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

    public String id() {
        return this.id;
    }

    public Person id(String id) throws ArgumentNullException {
        try {
            return new Person(id, username, email);
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("username isn't changing, so this error shouldn't occur");
        }
    }

    public String username() {
        return this.username;
    }

    public Person username(String username) throws InvalidLengthException {
        try {
            return new Person(id, username, email);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("id can't be null");
        }
    }

    public String email() {
        return this.email;
    }

    public Person email(String email) {
        try {
            return new Person(id, username, email);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("id can't be null");
        } catch (InvalidLengthException e) {
            throw new IllegalStateException("username can't be the wrong length");
        }
    }
}
