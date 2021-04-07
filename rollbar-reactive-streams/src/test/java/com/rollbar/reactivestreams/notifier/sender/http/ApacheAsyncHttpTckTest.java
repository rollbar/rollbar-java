package com.rollbar.reactivestreams.notifier.sender.http;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoSettings;
import org.mockito.testng.MockitoTestNGListener;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.*;

import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

// The reactivestreams TCK tests use TestNG but its class scanning doesn't work if the annotations are only
// present in the base class, we need at least one TestNG annotation on the derived class.
@Test
@Listeners(MockitoTestNGListener.class)
// The structure of the suite is that we just provide the publisher, and the TCK tests control the entire flow,
// so we don't really care if they call our stubbed methods or not.
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApacheAsyncHttpTckTest extends PublisherVerification<SimpleHttpResponse> {
  @Mock
  private HttpAsyncClient client;
  private ScheduledExecutorService executor;

  private final String url = "ignored";

  public ApacheAsyncHttpTckTest() {
    super(new TestEnvironment(1000, 1000));
  }

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    executor = Executors.newSingleThreadScheduledExecutor();
  }

  @AfterMethod
  public void tearDown() {
    executor.shutdown();
  }

  @Override
  public long maxElementsFromPublisher() {
    return 1;
  }

  @Override
  public Publisher<SimpleHttpResponse> createPublisher(long elements) {
    setupHttpResponse(callback -> {
      SimpleHttpResponse result = new SimpleHttpResponse(200);
      callback.completed(result);
      return result;
    });

    return new ApacheRequestPublisher(client, new AsyncHttpRequestImpl(url,
        new LinkedHashMap<String, String>().entrySet(), ""));
  }

  @Override
  public Publisher<SimpleHttpResponse> createFailedPublisher() {
    setupHttpResponse(callback -> {
      callback.failed(new SocketException("Test"));
      return null;
    });

    return new ApacheRequestPublisher(client, new AsyncHttpRequestImpl(url,
        new LinkedHashMap<String, String>().entrySet(), ""));
  }

  @Override
  @Ignore(
      "This test requires a publisher that fails without any demand (on subscribe), our publisher can only fail "
          + "after an element has been requested")
  public void optional_spec104_mustSignalOnErrorWhenFails() {
  }

  @Override
  @Ignore(
      "This test requires a publisher that fails without any demand (on subscribe), our publisher can only fail "
          + "after an element has been requested")
  public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe() {
  }

  private void setupHttpResponse(
      Function<FutureCallback<SimpleHttpResponse>, SimpleHttpResponse> action) {
    long delayMs = 5;
    doAnswer((Answer<Future<SimpleHttpResponse>>) invocation -> executor.schedule(() -> {
      FutureCallback<SimpleHttpResponse> callback = invocation.getArgument(4);
      return action.apply(callback);
    }, delayMs, TimeUnit.MILLISECONDS)).when(client).execute(any(), any(), any(), any(), any());
  }
}
