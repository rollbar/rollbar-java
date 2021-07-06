package com.rollbar.reactivestreams.reactor.example;

import com.rollbar.reactivestreams.notifier.ReactorRollbar;
import com.rollbar.reactivestreams.notifier.config.Config;
import com.rollbar.reactivestreams.notifier.config.ConfigBuilder;
import com.rollbar.reactivestreams.notifier.sender.http.ReactorAsyncHttpClient;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.tcp.TcpServer;

/**
 * Application example using rollbar-reactive-streams-reactor.
 */
public class Application {
  /**
   * Main method for the rollbar-reactive-streams-reactor example application.
   *
   * @param args command line arguments (unused).
   */
  public static void main(String[] args) throws Exception {
    Config config = ConfigBuilder
        .withAccessToken(System.getenv("ROLLBAR_ACCESSTOKEN"))
        .httpClient(new ReactorAsyncHttpClient.Builder().build())
        .environment("development")
        .build();

    ReactorRollbar rollbar = new ReactorRollbar(config);

    // We'll create a misbehaving HTTP server that immediately disconnects clients
    DisposableServer badServer = TcpServer.create().port(0)
        .doOnConnection(DisposableChannel::disposeNow)
        .bindNow();

    String url = "http://localhost:" + badServer.port();

    // Rollbar.logMonoError will log the error and return the original Mono
    Mono<HttpClientResponse> request = HttpClient.create()
        .get()
        .uri(url)
        .response()
        .onErrorResume(rollbar::logMonoError);

    // The Mono still contains the original error, and we're free to handle it however we see fit.
    // For this example, we'll replace the error it with an empty Mono to let the application
    // exit successfully, and block until it completes.
    request.onErrorResume(r -> Mono.empty()).block();

    rollbar.close(true);

    badServer.dispose();
  }

}
