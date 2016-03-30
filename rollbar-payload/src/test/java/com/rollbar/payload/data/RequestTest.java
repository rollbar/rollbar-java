package com.rollbar.payload.data;

import com.rollbar.testing.*;

import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by chris on 11/22/15.
 */
public class RequestTest {

    private Request r;
    private LinkedHashMap<String, String> headers;
    private LinkedHashMap<String, String> params;

    @Before
    public void setUp() throws Exception {
        r = new Request();
        headers = new LinkedHashMap<String, String>();
        headers.put("Hello", "World");
        params = new LinkedHashMap<String, String>();
        params.put("query", "where is carmen san diego?");
    }

    @Test
    public void testCopy() throws Exception {
        Request request = r.url("www.google.com")
                .method("METHOD")
                .headers(headers)
                .params(params)
                .put("Extra", "Value");
        Request copied = request.copy();
        Set<String> original = request.keys(false);
        Set<String> copy = copied.keys(false);
        List<String> origList = new ArrayList<String>(original);
        Collections.sort(origList);
        List<String> copyList = new ArrayList<String>(copy);
        Collections.sort(copyList);
        assertArrayEquals(origList.toArray(), copyList.toArray());
    }

    @Test
    public void testUrl() throws Exception {
        TestThat.getAndSetWorks(r, "www.google.com", "www.rollbar.com", new GetAndSet<Request, String>() {
            public String get(Request request) {
                return request.url();
            }

            public Request set(Request request, String val) {
                return request.url(val);
            }
        });
    }

    @Test
    public void testMethod() throws Exception {
        TestThat.getAndSetWorks(r, "method()", "getMethod()", new GetAndSet<Request, String>() {
            public String get(Request request) {
                return request.method();
            }

            public Request set(Request request, String val) {
                return request.method(val);
            }
        });
    }

    @Test
    public void testHeaders() throws Exception {
        LinkedHashMap<String, String> one = new LinkedHashMap<String, String>();
        one.put("Hello", "world");
        LinkedHashMap<String, String> two = new LinkedHashMap<String, String>();
        two.put("Goodbye", "universe");
        TestThat.getAndSetWorks(r, one, two, new GetAndSet<Request, Map<String, String>>() {
            public Map<String, String> get(Request request) {
                return request.headers();
            }

            public Request set(Request request, Map<String, String> val) {
                return request.headers(val);
            }
        });
    }

    @Test
    public void testParams() throws Exception {
        LinkedHashMap<String, String> one = new LinkedHashMap<String, String>();
        one.put("Hello", "world");
        LinkedHashMap<String, String> two = new LinkedHashMap<String, String>();
        two.put("Goodbye", "universe");
        TestThat.getAndSetWorks(r, one, two, new GetAndSet<Request, Map<String, String>>() {
            public Map<String, String> get(Request request) {
                return request.params();
            }

            public Request set(Request request, Map<String, String> val) {
                return request.params(val);
            }
        });
    }

    @Test
    public void testGet() throws Exception {
        LinkedHashMap<String, String> one = new LinkedHashMap<String, String>();
        one.put("Hello", "world");
        LinkedHashMap<String, String> two = new LinkedHashMap<String, String>();
        two.put("Goodbye", "universe");
        TestThat.getAndSetWorks(r, one, two, new GetAndSet<Request, Map<String, String>>() {
            public Map<String, String> get(Request request) {
                return request.getGet();
            }

            public Request set(Request request, Map<String, String> val) {
                return request.setGet(val);
            }
        });
    }

    @Test
    public void testQueryString() throws Exception {
        TestThat.getAndSetWorks(r, "CanIHazCheezbrgr?", "Wherefore art thou Romeo?", new GetAndSet<Request, String>() {
            public String get(Request request) {
                return request.queryString();
            }

            public Request set(Request request, String val) {
                return request.queryString(val);
            }
        });
    }

    @Test
    public void testPost() throws Exception {
        LinkedHashMap<String, Object> one = new LinkedHashMap<String, Object>();
        one.put("Hello", "world");
        one.put("TMOLTUAE", 42);
        LinkedHashMap<String, Object> two = new LinkedHashMap<String, Object>();
        two.put("Goodbye", "universe");
        two.put("pi", 3.1415);
        TestThat.getAndSetWorks(r, one, two, new GetAndSet<Request, Map<String, Object>>() {
            public Map<String, Object> get(Request request) {
                return request.post();
            }

            public Request set(Request request, Map<String, Object> val) {
                return request.post(val);
            }
        });
    }

    @Test
    public void testBody() throws Exception {
        TestThat.getAndSetWorks(r, "OF LIES", "OF WATER", new GetAndSet<Request, String>() {
            public String get(Request request) {
                return request.body();
            }

            public Request set(Request request, String val) {
                return request.body(val);
            }
        });
    }

    @Test
    public void testUserIp() throws Exception {
        TestThat.getAndSetWorks(r, InetAddress.getByName("www.google.com"), InetAddress.getByName("localhost"), new GetAndSet<Request, InetAddress>() {
            public InetAddress get(Request request) {
                return request.userIp();
            }

            public Request set(Request request, InetAddress val) {
                return request.userIp(val);
            }
        });
    }

    @Test
    public void testConstructor() throws Exception {
        String url = "www.rollbar.com/api/1/items_post?custom_param=10";
        LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Location", "www.rollabr.com");
        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("custom_param", "10");
        LinkedHashMap<String, Object> post = new LinkedHashMap<String, Object>();
        post.put("access_token", "bcde15ef019347bafe1230000");
        post.put("data", new LinkedHashMap<String, Object>()); // Truncated a bit ;)
        InetAddress userIp = InetAddress.getByName("localhost");
        Request r = new Request(url, "POST", headers, params, null, "?custom_param=10", post, "{ \"access_token\": \"bcde15ef019347bafe1230000\", \"data\": {} }", userIp);
        assertEquals(url, r.url());
        assertEquals("POST", r.method());
        // As long as it's not null, the rest of this gets tested elsewhere.
    }
}