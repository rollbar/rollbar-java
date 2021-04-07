package com.rollbar.reactivestreams.notifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.rollbar.notifier.sender.Sender;
import com.rollbar.reactivestreams.notifier.sender.AsyncSender;
import com.rollbar.reactivestreams.notifier.sender.http.ReactorAsyncHttpClient;
import java.net.Proxy;
import org.mockito.stubbing.Answer;

/**
 * <p>
 * Run the {@link com.rollbar.notifier.sender.SyncSender} integration tests once again, this time
 * using the reactor HTTP client.
 * </p>
 */
public class SyncSenderWrapperReactorITest extends com.rollbar.notifier.RollbarITest {
  @Override
  protected Sender buildSender(String url, String accessToken, Proxy proxy) {
    ReactorAsyncHttpClient.Builder httpBuilder = new ReactorAsyncHttpClient.Builder();

    if (proxy != null) {
      httpBuilder = httpBuilder.proxy(proxy);
    }

    AsyncSender.Builder builder = new AsyncSender.Builder(httpBuilder.build(), url);

    if (accessToken != null) {
      builder = builder.accessToken(accessToken);
    }

    Sender realSender = builder.buildSync();

    // Copied and pasted from rollbar-reactive-streams to avoid creating gradle test artifacts
    // in that project just to reuse a couple of small snippets of code.
    // Once we figure out how to make the gradle integration test plugin get along with the
    // java-test-fixtures plugin, we'll remove this duplicate code.
    BlockingListener blockingListener = new BlockingListener(5000);
    realSender.addListener(blockingListener);

    Sender senderSpy = spy(realSender);

    doAnswer((Answer<Void>) invocation -> {
      blockingListener.clear();
      realSender.send(invocation.getArgument(0));
      blockingListener.block();
      return null;
    }).when(senderSpy).send(any());

    return senderSpy;
  }
}
