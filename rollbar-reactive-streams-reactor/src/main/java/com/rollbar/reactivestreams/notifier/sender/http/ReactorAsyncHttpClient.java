package com.rollbar.reactivestreams.notifier.sender.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

/**
 * <a href="https://projectreactor.io/">Project Reactor</a> {@link AsyncHttpClient} implementation.
 */
public class ReactorAsyncHttpClient implements AsyncHttpClient {
  private final RequestDrainSignal runningRequests;
  private final HttpClient httpClient;

  ReactorAsyncHttpClient(Builder builder) {
    this.runningRequests = new RequestDrainSignal();

    HttpClient httpClient;
    if (builder.connectionProvider != null) {
      httpClient = HttpClient.create(builder.connectionProvider);
    } else {
      httpClient = HttpClient.create();
    }

    ProxyProvider.Proxy proxyType = getProxyType(builder.proxy);
    InetSocketAddress proxyAddress = getProxyAddress(proxyType, builder.proxy);

    if (proxyType != null) {
      httpClient = httpClient.proxy(
          typeSpec -> typeSpec.type(proxyType).address(proxyAddress));
    }

    this.httpClient = httpClient.compress(true);
  }

  @Override
  public Publisher<AsyncHttpResponse> send(AsyncHttpRequest httpRequest) {

    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
    buffer.writeCharSequence(httpRequest.getBody(), StandardCharsets.UTF_8);
    Mono<ByteBuf> buf = Mono.just(buffer);

    return httpClient
        .headers(entries -> {
          for (Map.Entry<String, String> header : httpRequest.getHeaders()) {
            entries.add(header.getKey(), header.getValue());
          }
        })
        .post()
        .uri(httpRequest.getUrl())
        .send(buf)
        .responseSingle((res, content) ->
            content.map(body -> new AbstractMap.SimpleEntry<>(res, body)))
        .doOnSubscribe(sig -> runningRequests.increment())
        .doAfterTerminate(runningRequests::decrement)
        .map(ReactorAsyncHttpClient::mapResponse);
  }

  private static AsyncHttpResponse mapResponse(
      AbstractMap.SimpleEntry<HttpClientResponse, ByteBuf> response) {
    return new AsyncHttpResponse() {
      @Override
      public int getStatusCode() {
        return response.getKey().status().code();
      }

      @Override
      public Iterable<Map.Entry<String, String>> getHeaders() {
        return response.getKey().responseHeaders();
      }

      @Override
      public String getBody() {
        return response.getValue().toString(StandardCharsets.UTF_8);
      }
    };
  }

  @Override
  public void close() {
    this.close(false);
  }

  @Override
  public void close(boolean wait) {
    // We can't shutdown the IO reactor from here, it's either provided by the user via the
    // connection provider, or it's managed internally by the HTTP client and GCd later.
    // We can wait for requests to complete though.

    if (wait) {
      // Project Reactor marks the reactor threads, and then makes sure blocking methods are
      // not called from those threads. Our close methods are only meant to be used at shutdown,
      // outside of high throughput reactor activity, so using Mono.block here will make it very
      // visible if users accidentally call this from the reactive parts of their application.
      Mono.from(runningRequests.toPublisher()).block();
    }
  }

  private static InetSocketAddress getProxyAddress(ProxyProvider.Proxy proxyType, Proxy proxy) {
    if (proxy == null) {
      return null;
    }

    if (proxyType == null) {
      return null;
    }

    if (proxy.address() instanceof InetSocketAddress) {
      return (InetSocketAddress) proxy.address();
    } else {
      throw new IllegalArgumentException("Only InetSocketAddress proxy addresses are "
          + "supported, " + proxy.address() + " was provided");
    }
  }

  private static ProxyProvider.Proxy getProxyType(Proxy proxy) {
    if (proxy == null) {
      return null;
    }

    switch (proxy.type()) {
      case DIRECT:
        return null;
      case HTTP:
        return ProxyProvider.Proxy.HTTP;
      case SOCKS:
        return ProxyProvider.Proxy.SOCKS5;
      default:
        throw new IllegalArgumentException("Unknown proxy type " + proxy.type());
    }
  }

  public static final class Builder {
    private Proxy proxy;
    private ConnectionProvider connectionProvider;

    /**
     * The {@link Proxy proxy} to be used to send the data.
     *
     * @param proxy the proxy.
     * @return the builder instance.
     */
    public Builder proxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * The {@link ConnectionProvider} used to establish HTTP connections.
     *
     * @param connectionProvider the connection provider
     * @return the builder instance.
     */
    public Builder connectionProvider(ConnectionProvider connectionProvider) {
      this.connectionProvider = connectionProvider;
      return this;
    }

    /**
     * Builds the {@link ReactorAsyncHttpClient} HTTP client.
     *
     * @return the HTTP client.
     */
    public ReactorAsyncHttpClient build() {
      return new ReactorAsyncHttpClient(this);
    }
  }

}
