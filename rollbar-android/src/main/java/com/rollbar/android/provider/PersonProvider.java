package com.rollbar.android.provider;

import com.rollbar.api.payload.data.Person;
import com.rollbar.notifier.provider.Provider;

public class PersonProvider implements Provider<Person> {
    private final Person person;

    public PersonProvider(final String id, final String username, final String email) {
        this.person = new Person.Builder()
                .id(id)
                .username(username)
                .email(email)
                .build();
    }

    public PersonProvider(final String id) {
        this(id, null, null);
    }

    public PersonProvider() {
        this(null, null, null);
    }

    @Override
    public Person provide() {
        return person;
    }
}