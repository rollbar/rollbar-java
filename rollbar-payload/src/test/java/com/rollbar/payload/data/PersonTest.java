package com.rollbar.payload.data;

import com.rollbar.testing.GetAndSet;
import com.rollbar.testing.TestThat;
import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.InvalidLengthException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PersonTest {

    private Person p;

    @Before
    public void setUp() throws Exception {
        p = new Person("userid-123", "billy-bob", "bbob@gmail.com");
    }

    @Test
    public void constructorWorks() throws Exception {
        Person person = null;
        try {
            person = new Person("hello-id");
        } catch (ArgumentNullException e) {
            fail("Id isn't null");
        }
        assertEquals("hello-id", person.id());
        assertNull(person.username());
        assertNull(person.email());
    }

    @Test(expected = ArgumentNullException.class)
    public void testIdNull() throws Exception {
        p.id(null);
    }

    @Test
    public void testId() throws Exception {
        TestThat.getAndSetWorks(p, "1234abcd", "efgh5678", new GetAndSet<Person, String>() {
            public String get(Person item) {
                return item.id();
            }

            public Person set(Person item, String val) {
                try {
                    return item.id(val);
                } catch (ArgumentNullException e) {
                    fail("Nothing is null");
                }
                return null;
            }
        });
    }

    @Test(expected = InvalidLengthException.class)
    public void testUsernameTooLong() throws Exception {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < 100; i++) builder.append("abcdefghijklmnopqrstuvwxyz");
        p.username(builder.toString());
    }

    @Test
    public void testUsername() throws Exception {
        TestThat.getAndSetWorks(p, "1234abcd", "efgh5678", new GetAndSet<Person, String>() {
            public String get(Person item) {
                return item.username();
            }

            public Person set(Person item, String val) {
                try {
                    return item.username(val);
                } catch (InvalidLengthException e) {
                    fail("Neither is wrong");
                }
                return null;
            }
        });
    }

    @Test
    public void testEmail() throws Exception {
        TestThat.getAndSetWorks(p, "1234abcd", "efgh5678", new GetAndSet<Person, String>() {
            public String get(Person item) {
                return item.email();
            }

            public Person set(Person item, String val) {
                return item.email(val);
            }
        });
    }
}