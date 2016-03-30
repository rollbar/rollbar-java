package com.rollbar.payload.data;

import com.rollbar.testing.GetAndSet;
import com.rollbar.testing.TestThat;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by chris on SetSet/20/Set5.
 */
public class ServerTest {
    private Server server = new Server();

    @Before
    public void setUp() {
        server = new Server();
    }

    @Test
    public void testHost() throws Exception {
        TestThat.getAndSetWorks(server, "host-a", "host-b", new GetAndSet<Server, String>() {
            public String get(Server item) {
                return item.host();
            }

            public Server set(Server item, String val) {
                return item.host(val);
            }
        });
    }

    @Test
    public void testRoot() throws Exception {
        TestThat.getAndSetWorks(server, "root-a", "root-b", new GetAndSet<Server, String>() {
            public String get(Server item) {
                return item.root();
            }

            public Server set(Server item, String val) {
                return item.root(val);
            }
        });
    }

    @Test
    public void testBranch() throws Exception {
        TestThat.getAndSetWorks(server, "branch-a", "branch-b", new GetAndSet<Server, String>() {
            public String get(Server item) {
                return item.branch();
            }

            public Server set(Server item, String val) {
                return item.branch(val);
            }
        });
    }

    @Test
    public void testCodeVersion() throws Exception {
        TestThat.getAndSetWorks(server, "code-version-a", "code-version-b", new GetAndSet<Server, String>() {
            public String get(Server item) {
                return item.codeVersion();
            }

            public Server set(Server item, String val) {
                return item.codeVersion(val);
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutReserved() throws Exception {
        Server s = server.put(Server.HOST_KEY, "host-name");
    }

    @Test
    public void testGetPut() throws Exception {
        TestThat.getAndSetWorks(server, 1024, 16, new GetAndSet<Server, Integer>() {
            public Integer get(Server item) {
                return (Integer) item.get("extra");
            }

            public Server set(Server item, Integer val) {
                return item.put("extra", val);
            }
        });
    }

    @Test
    public void testPutAfterSet() {
        Server s = server.branch("Branch")
                .put("extra", "fun");
        assertEquals("Branch", s.branch());
        assertEquals("fun", s.get("extra"));
    }

    @Test
    public void testKeys() throws Exception {
        Server s = server.branch("branch")
                .root("root")
                .put("extra", 15);
        ArrayList<String> keys = new ArrayList<String>(s.keys(true));
        java.util.Collections.sort(keys);
        String[] actual = keys.toArray(new String[keys.size()]);
        String[] expected = new String[] { "extra" };
        assertArrayEquals(expected, actual);

        keys =  new ArrayList<String>(s.keys(false));
        java.util.Collections.sort(keys);
        actual = keys.toArray(new String[keys.size()]);
        expected = new String[] { Server.BRANCH_KEY, Server.CODE_VERSION_KEY, "extra", Server.HOST_KEY, Server.ROOT_KEY };
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testConstructor() {
        Server s = new Server("Host", "Root", "Branch", "Code");
        assertEquals("Host", s.host());
        assertEquals("Root", s.root());
        assertEquals("Branch", s.branch());
        assertEquals("Code", s.codeVersion());
        assertEquals(4, s.keys(false).size());
    }
}