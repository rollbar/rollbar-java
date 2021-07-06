package com.rollbar.reactivestreams.notifier.sender.http;

import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;

public class AsyncHttpClientFactoryTest {
  @Test
  public void ifApacheIsInClasspathItShouldReturnApacheClient() {
    assertThat(AsyncHttpClientFactory.defaultClient(), Matchers.instanceOf(ApacheAsyncHttpClient.class));
  }
}
