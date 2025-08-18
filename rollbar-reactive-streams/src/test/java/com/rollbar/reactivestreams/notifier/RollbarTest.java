package com.rollbar.reactivestreams.notifier;

import static com.rollbar.reactivestreams.notifier.config.ConfigBuilder.withAccessToken;
import static com.rollbar.reactivestreams.notifier.config.ConfigBuilder.withConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.util.BodyFactory;
import com.rollbar.reactivestreams.notifier.config.Config;
import com.rollbar.reactivestreams.notifier.sender.Sender;

import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.ConnectionClosedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import reactor.core.publisher.Mono;

public class RollbarTest {
  static final String ACCESS_TOKEN = "access_token";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  Sender sender;

  @Mock
  BodyFactory bodyFactory;

  @Mock
  Response response;

  Config config;

  @Before
  public void setUp() {
    when(sender.send(any())).thenReturn(Mono.just(response));

    config = withAccessToken(ACCESS_TOKEN)
        .sender(sender)
        .build();
  }

  @Test
  public void shouldDoNothingIfDisabled() {
    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    String description = "description";
    Map<String, Object> custom = new HashMap<>();

    Config config = withConfig(this.config)
        .enabled(false)
        .build();

    Rollbar sut = new Rollbar(config, bodyFactory);

    Mono.from(sut.log(error, custom, description, level)).block();

    verify(sender, never()).send(any());
  }

  @Test
  public void shouldNotThrowExceptionWithEmptyConfig() {
    Config config = withAccessToken("access_token").build();

    Rollbar sut = new Rollbar(config);

    Throwable error = new RuntimeException("Something went wrong.");

    Mono.from(sut.log(error, null, null, Level.ERROR))
      .onErrorResume(ex -> {
          if (ex instanceof ConnectionClosedException) {
            return Mono.empty();
          } else {
            return Mono.error(ex);
          }
        }
      )
      .block();
  }

  @Test
  public void shouldInitializeTheInternalInstanceOnce() {
    Config config = withAccessToken(ACCESS_TOKEN).build();

    Rollbar sut = Rollbar.init(config);
    Rollbar other = Rollbar.init(config);

    assertThat(sut, sameInstance(other));
  }

  @Test
  public void shouldNotThrowExceptionIfErrorProcessing() {
    Config config = withAccessToken("access_token")
        .transformer(data -> {
          throw new RuntimeException("Unexpected error.");
        })
        .build();

    Rollbar sut = new Rollbar(config);

    Throwable error = new RuntimeException("Something went wrong.");

    Mono.from(sut.log(error, null, null, Level.ERROR)).block();
  }

  @Test
  public void shouldClose() throws Exception {
    Rollbar sut = new Rollbar(config, bodyFactory);

    sut.close(true);
    verify(sender).close(true);

    sut.close(false);
    verify(sender).close(false);
  }

}
