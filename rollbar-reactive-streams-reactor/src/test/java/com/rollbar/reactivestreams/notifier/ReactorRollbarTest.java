package com.rollbar.reactivestreams.notifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.body.BodyContent;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.reactivestreams.notifier.config.Config;
import com.rollbar.reactivestreams.notifier.config.ConfigBuilder;
import com.rollbar.reactivestreams.notifier.sender.AsyncSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

public class ReactorRollbarTest {
  private ReactorRollbar rollbar;
  private AsyncSender sender;

  @BeforeEach
  private void setUp() {
    sender = mock(AsyncSender.class);
    Response response = mock(Response.class);
    when(sender.send(any())).thenReturn(Mono.just(response));

    Config config = ConfigBuilder
        .withAccessToken("TEST TOKEN")
        .sender(sender)
        .environment("development")
        .build();

    rollbar = new ReactorRollbar(config);
  }

  @Test
  public void onMonoErrorShouldReportMonoErrors() {
    Mono<String> sut = Mono.<String>error(new TestException("Test Exception"))
        .onErrorResume(rollbar::logMonoError);

    StepVerifier.create(sut)
        .expectError(TestException.class)
        .verify();

    verify(sender, times(1)).send(any());
  }

  @Test
  public void onMonoSuccessShouldReturnMonoValue() {
    Mono<String> sut = Mono.just("successValue")
        .onErrorResume(rollbar::logMonoError);

    StepVerifier.create(sut)
        .expectNext("successValue")
        .expectComplete()
        .verify();

    verify(sender, times(0)).send(any());
  }

  @Test
  public void onFluxErrorShouldReportFluxError() {
    Flux<String> sut = Flux.<String>error(new TestException("Test Exception"))
        .onErrorResume(rollbar::logFluxError);

    StepVerifier.create(sut)
        .expectError(TestException.class)
        .verify();

    verify(sender, times(1)).send(any());
  }

  @Test
  public void onFluxSuccessShouldReturnFluxValues() {
    Flux<String> sut = Flux.just("successValue1", "successValue2", "successValue3")
        .onErrorResume(rollbar::logFluxError);

    StepVerifier.create(sut)
        .expectNext("successValue1")
        .expectNext("successValue2")
        .expectNext("successValue3")
        .expectComplete()
        .verify();

    verify(sender, times(0)).send(any());
  }

  @Test
  public void whenExceptionIsThrownItShouldCaptureTrace() {
    Mono<String> sut = Mono.fromSupplier(ReactorRollbarTest::makeStringA);

    sut.onErrorResume(rollbar::logMonoError)
            .onErrorResume(ignored -> Mono.empty())
            .block();

    ArgumentCaptor<Payload> arg = ArgumentCaptor.forClass(Payload.class);

    verify(sender).send(arg.capture());

    BodyContent value = arg.getValue().getData().getBody().getContents();

    assertThat(value, instanceOf(Trace.class));
    
    Trace trace = (Trace)value;

    List<Frame> frames = trace.getFrames();
    assertThat(frames.get(frames.size() - 1).getMethod(), equalTo("makeStringB"));
    assertThat(frames.get(frames.size() - 2).getMethod(), equalTo("makeStringA"));
  }
  
  private static String makeStringA() {
    return makeStringB();
  }

  private static String makeStringB() {
    throw new IllegalStateException("Unable to make string");
  }

  private static class TestException extends RuntimeException {
    public TestException(String msg) {
      super(msg);
    }
  }
}
