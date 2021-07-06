package com.rollbar.reactivestreams.notifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.rollbar.notifier.sender.Sender;
import com.rollbar.reactivestreams.notifier.sender.AsyncSender;
import com.rollbar.reactivestreams.notifier.sender.http.ApacheAsyncHttpClient;
import java.net.Proxy;
import org.mockito.stubbing.Answer;

/**
 * <p>
 * Run the {@link com.rollbar.notifier.sender.SyncSender} tests using the async-to-sync wrapper,
 * which should implement the same behaviour. This test uses the built-in Apache HTTP Components
 * client.
 * </p>
 */
public class SyncSenderWrapperITest extends com.rollbar.notifier.RollbarITest {
  @Override
  protected Sender buildSender(String url, String accessToken, Proxy proxy) {
    ApacheAsyncHttpClient.Builder httpBuilder = new ApacheAsyncHttpClient.Builder();

    if (proxy != null) {
      httpBuilder = httpBuilder.proxy(proxy);
    }

    AsyncSender.Builder builder = new AsyncSender.Builder(httpBuilder.build(), url);

    if (accessToken != null) {
      builder = builder.accessToken(accessToken);
    }

    Sender realSender = builder.buildSync();

    BlockingListener blockingListener = new BlockingListener(5000);
    realSender.addListener(blockingListener);

    Sender senderSpy = spy(realSender);

    // Add some additional blocking to ensure requests are completed by the time the send method
    // returns, since that's what the tests assume.
    doAnswer((Answer<Void>) invocation -> {
      blockingListener.clear();
      realSender.send(invocation.getArgument(0));
      blockingListener.block();
      return null;
    }).when(senderSpy).send(any());

    return senderSpy;
  }
}
