package com.rollbar.reactivestreams.notifier.sender;

import com.google.gson.Gson;
import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpClient;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpRequest;
import com.rollbar.reactivestreams.notifier.sender.http.AsyncHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

public class AsyncSenderTest {
  private static final String ACCESS_TOKEN = "1234";

  @Rule
  public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private AsyncHttpClient httpClient;

  @Mock
  private AsyncHttpResponse response;

  private AsyncSender sender;

  @Before
  public void setUp() {
    when(response.getBody()).thenReturn("{\"err\": 0, \"uuid\": \"1234\" }");
    when(response.getStatusCode()).thenReturn(201);

    sender = new AsyncSender.Builder(httpClient)
            .accessToken(ACCESS_TOKEN)
            .build();
  }
  
  @Test 
  public void ifSendOperationFailsItShouldReturnErrorPublisher() {
    when(httpClient.send(any())).thenReturn(Mono.error(new IllegalStateException("Bad time to call this method")));

    Payload payload = createPayload();

    CaptureSubscriber<Response> asyncCapture = new CaptureSubscriber<>();

    sender.send(payload).subscribe(asyncCapture);

    asyncCapture.blockWithoutThrowing();
    
    assertThat(asyncCapture.terminated, equalTo(true));
    assertThat(asyncCapture.error, notNullValue());
    assertThat(asyncCapture.error, instanceOf(IllegalStateException.class));
    assertThat(asyncCapture.error.getMessage(), equalTo("Bad time to call this method"));
  }

  @Test
  public void ifSendOperationSucceedsItShouldReturnResponse() {
    AsyncHttpRequest tokenMatcher = argThat(item -> {
      if (item == null) {
        return false;
      }
      Map<String, Object> result = jsonToMap(item.getBody());
      return result.containsKey("access_token") && ACCESS_TOKEN.equals(result.get("access_token"));
    });

    when(httpClient.send(tokenMatcher)).thenReturn(Mono.just(response));

    Payload payload = createPayload();

    CaptureSubscriber<Response> asyncCapture = new CaptureSubscriber<>();

    sender.send(payload).subscribe(asyncCapture);

    asyncCapture.blockWithoutThrowing();
    
    assertThat(asyncCapture.terminated, equalTo(true));
    assertThat(asyncCapture.error, nullValue());
    assertThat(asyncCapture.value.getResult().getErr(), equalTo(0));
    assertThat(asyncCapture.value.getResult().getContent(), equalTo("1234"));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> jsonToMap(String body) {
    return (Map<String, Object>) new Gson().fromJson(body, Map.class);
  }

  private Payload createPayload() {
    Data data = new Data.Builder()
            .environment("development")
            .codeVersion("0.2.33")
            .platform("macos")
            .language("java")
            .framework("reactive-streams")
            .context("context")
            .level(Level.DEBUG)
            .build();

    return new Payload.Builder()
            .accessToken(ACCESS_TOKEN)
            .data(data)
            .build();
  }

  private static class CaptureSubscriber<T> implements Subscriber<T> {
    volatile T value = null;
    volatile boolean terminated = false;
    volatile Throwable error = null;

    @Override
    public void onSubscribe(Subscription s) {
      s.request(1);
    }

    @Override
    public void onNext(T unused) {
      value = unused;
    }

    @Override
    public void onError(Throwable t) {
      error = t;
      terminated = true;
    }

    @Override
    public void onComplete() {
      terminated = true;
    }
    
    public void blockWithoutThrowing() {
      while (!terminated) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
