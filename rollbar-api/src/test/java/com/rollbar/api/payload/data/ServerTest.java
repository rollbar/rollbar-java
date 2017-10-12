package com.rollbar.api.payload.data;

import static com.rollbar.test.Factory.server;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ServerTest {

  @Test
  public void shouldBeEqual() {
    Server server1 = server();
    Server server2 = server();

    assertThat(server2, is(server1));
  }

  @Test
  public void shouldReturnAsJson() {
    Server server = server();

    Map<String, Object> expected = new HashMap<>();

    if (server.getHost() != null) {
      expected.put("host", server.getHost());
    }
    if (server.getRoot() != null) {
      expected.put("root", server.getRoot());
    }
    if (server.getBranch() != null) {
      expected.put("branch", server.getBranch());
    }
    if (server.getCodeVersion() != null) {
      expected.put("code_version", server.getCodeVersion());
    }

    assertThat(server.asJson(), is(expected));
  }
}