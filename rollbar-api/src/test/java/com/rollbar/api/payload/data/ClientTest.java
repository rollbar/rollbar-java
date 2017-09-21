package com.rollbar.api.payload.data;

import static com.rollbar.test.Factory.client;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

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

    assertThat(client.asJson(), is(client.getData()));
  }
}