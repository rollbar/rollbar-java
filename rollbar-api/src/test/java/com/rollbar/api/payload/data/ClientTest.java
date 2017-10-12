package com.rollbar.api.payload.data;

import static com.rollbar.test.Factory.client;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ClientTest {

  @Test
  public void shouldBeEqual() {
    Client client1 = client();
    Client client2 = client();

    assertThat(client2, is(client1));
  }

  @Test
  public void shouldReturnAsJson() {
    Client client = client();

    Map<String, Object> expected = new HashMap<>();

    if(client.getData() != null) expected.putAll(client.getData());
    if(client.getTopLevelData() != null) expected.putAll(client.getTopLevelData());

    assertThat(client.asJson(), is(expected));
  }
}