package com.rollbar.reactivestreams.notifier.sender.http;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class AsyncHttpClientFactoryTest {
  @Test
  public void ifReactorIsInClasspathItShouldReturnReactorClient() {
    assertThat(AsyncHttpClientFactory.defaultClient(), Matchers.instanceOf(ReactorAsyncHttpClient.class));
  }
}
