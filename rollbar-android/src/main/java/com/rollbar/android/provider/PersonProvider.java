package com.rollbar.android.provider;

import com.rollbar.api.payload.data.Person;
import com.rollbar.notifier.provider.Provider;

public class PersonProvider implements Provider<Person> {
    private Person person;

    @Override
    public Person provide() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
