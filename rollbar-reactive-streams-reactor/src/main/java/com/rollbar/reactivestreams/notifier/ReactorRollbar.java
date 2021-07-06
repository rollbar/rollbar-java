package com.rollbar.reactivestreams.notifier;

import com.rollbar.reactivestreams.notifier.config.Config;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 *   Async, non-blocking Rollbar notifier with additional helper methods to handle {@link Mono} and
 *   {@link Flux} errors.
 * </p>
 */
public class ReactorRollbar extends Rollbar {
  public ReactorRollbar(Config config) {
    super(config);
  }

  /**
   * Logs the error to Rollbar and returns the original {@link Mono}.
   *
   * @param t The error that occurred
   * @param <T> The type of elements of the Mono
   * @param <E> The type of the error
   * @return The original success or failure {@link Mono}, after logging the error to Rollbar
   */
  public <T, E extends Throwable> Mono<? extends T> logMonoError(E t) {
    return Mono.from(error(t)).flatMap(ignored -> Mono.error(t));
  }

  /**
   * Logs the error to Rollbar and returns the original {@link Flux}.
   *
   * @param t The error that occurred
   * @param <T> The type of elements of the Flux
   * @param <E> The type of the error
   * @return The original success or failure {@link Flux}, after logging the error to Rollbar
   */
  public <T, E extends Throwable> Flux<? extends T> logFluxError(E t) {
    return Flux.from(error(t)).flatMap(ignored -> Flux.error(t));
  }
}
