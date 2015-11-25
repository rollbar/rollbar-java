package com.rollbar.payload.data;

import com.rollbar.GetAndSet;
import com.rollbar.TestThat;
import com.rollbar.payload.data.body.Body;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.InvalidLengthException;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by chris on 11/21/15.
 */
public class DataTest {
    private Data d;

    @Before
    public void setUp() throws Exception {
        d = new Data("environment", Body.fromString("HI!"));
    }

    @Test
    public void testEnvironment() throws Exception {
        TestThat.getAndSetWorks(d, "production", "development", new GetAndSet<Data, String>() {
            public String get(Data item) {
                return item.environment();
            }

            public Data set(Data item, String val) {
                try {
                    return item.environment(val);
                } catch (ArgumentNullException e) {
                    fail("neither is null");
                } catch (InvalidLengthException e) {
                    fail("neighter is too long");
                }
                return null;
            }
        });
    }

    @Test
    public void testBody() throws Exception {
        Body one = Body.fromString("Hello!");
        Body two = Body.fromString("Goodbye!");
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, Body>() {
            public Body get(Data item) {
                return item.body();
            }

            public Data set(Data item, Body val) {
                try {
                    return item.body(val);
                } catch (ArgumentNullException e) {
                    fail("neither is null");
                }
                return null;
            }
        });
    }

    @Test
    public void testLevel() throws Exception {
        Level one = Level.CRITICAL;
        Level two = Level.INFO;
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, Level>() {
            public Level get(Data data) {
                return data.level();
            }

            public Data set(Data data, Level val) {
                return data.level(val);
            }
        });
    }

    @Test
    public void testTimestamp() throws Exception {
        Instant one = Instant.ofEpochSecond(Instant.now().getEpochSecond());
        Instant two = one.minus(7, ChronoUnit.DAYS);
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, Instant>() {
            public Instant get(Data data) {
                return data.timestamp();
            }

            public Data set(Data data, Instant val) {
                return data.timestamp(val);
            }
        });
    }

    @Test
    public void testCodeVersion() throws Exception {
        String one = "sha-1";
        String two = "sha-2";
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, String>() {
            public String get(Data data) {
                return data.codeVersion();
            }

            public Data set(Data data, String val) {
                return data.codeVersion(val);
            }
        });
    }

    @Test
    public void testPlatform() throws Exception {
        String one = "windows";
        String two = "mac";
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, String>() {
            public String get(Data data) {
                return data.platform();
            }

            public Data set(Data data, String val) {
                return data.platform(val);
            }
        });
    }

    @Test
    public void testLanguage() throws Exception {
        String one = "java";
        String two = "clojure";
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, String>() {
            public String get(Data data) {
                return data.language();
            }

            public Data set(Data data, String val) {
                return data.language(val);
            }
        });
    }

    @Test
    public void testFramework() throws Exception {
        String one = "play";
        String two = "spring";
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, String>() {
            public String get(Data data) {
                return data.framework();
            }

            public Data set(Data data, String val) {
                return data.framework(val);
            }
        });
    }

    @Test
    public void testContext() throws Exception {
        String one = "POST /user";
        String two = "GET /settings";
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, String>() {
            public String get(Data data) {
                return data.context();
            }

            public Data set(Data data, String val) {
                return data.context(val);
            }
        });
    }

    @Test
    public void testRequest() throws Exception {
        Request one = new Request().url("https://www.rollbar.com");
        Request two = new Request().method("getRootPath(String path, Request context)");
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, Request>() {
            public Request get(Data data) {
                return data.request();
            }

            public Data set(Data data, Request val) {
                return data.request(val);
            }
        });
    }

    @Test
    public void testPerson() throws Exception {
        Person one = new Person("id");
        Person two = new Person("other_id");
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, Person>() {
            public Person get(Data data) {
                return data.person();
            }

            public Data set(Data data, Person val) {
                return data.person(val);
            }
        });
    }

    @Test
    public void testServer() throws Exception {
        Server one = new Server().host("web-server-a");
        Server two = new Server().host("web-server-b");
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, Server>() {
            public Server get(Data data) {
                return data.server();
            }

            public Data set(Data data, Server val) {
                return data.server(val);
            }
        });
    }

    @Test
    public void testCustom() throws Exception {
        HashMap<String, Object> one = new HashMap<String, Object>();
        one.put("test", 1);
        HashMap<String, Object> two = new HashMap<String, Object>();
        two.put("Another", "HI THERE!");
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, HashMap<String, Object>>() {
            public HashMap<String, Object> get(Data data) {
                return data.custom();
            }

            public Data set(Data data, HashMap<String, Object> val) {
                return data.custom(val);
            }
        });
    }

    @Test
    public void testFingerprint() throws Exception {
        String one = "12345";
        String two = "67890";
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, String>() {
            public String get(Data data) {
                return data.fingerprint();
            }

            public Data set(Data data, String val) {
                return data.fingerprint(val);
            }
        });
    }

    @Test
    public void testTitle() throws Exception {
        String one = "eric";
        String two = "henry";
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, String>() {
            public String get(Data data) {
                return data.title();
            }

            public Data set(Data data, String val) {
                try {
                    return data.title(val);
                } catch (InvalidLengthException e) {
                    fail("Neither title is null");
                    return null;
                }
            }
        });
    }

    @Test
    public void testUuid() throws Exception {
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, UUID>() {
            public UUID get(Data data) {
                return data.uuid();
            }

            public Data set(Data data, UUID val) {
                return data.uuid(val);
            }
        });
    }

    @Test
    public void testNotifier() throws Exception {
        Notifier one = new Notifier();
        Notifier two = new Notifier("derivative-one", "1.2.3");
        TestThat.getAndSetWorks(d, one, two, new GetAndSet<Data, Notifier>() {
            public Notifier get(Data data) {
                return data.notifier();
            }

            public Data set(Data data, Notifier val) {
                return data.notifier(val);
            }
        });
    }

    @Test
    public void testConstructor() throws Exception {
        Data d = new Data("environment", Body.fromString("String"), Level.CRITICAL, Instant.now(), "version", "platform", "language", "framework", "CONTEXT", new Request(), new Person("ID"), new Server(), new HashMap<String, Object>(), "fingerprint", "title", UUID.randomUUID(), new Notifier());
    }
}