package com.rollbar.reactivestreams.notifier.sender.http;

import com.rollbar.reactivestreams.Utils;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.io.CloseMode;
import org.reactivestreams.Publisher;

/**
 * Apache HTTP Components based, non-blocking HTTP client.
 */
public class ApacheAsyncHttpClient implements AsyncHttpClient {
  private final CloseableHttpAsyncClient client;

  ApacheAsyncHttpClient(Builder builder) {
    HttpAsyncClientBuilder clientBuilder = HttpAsyncClientBuilder.create();

    clientBuilder = setProxy(clientBuilder, builder.proxy);

    if (builder.connectionManager != null) {
      clientBuilder = clientBuilder.setConnectionManager(builder.connectionManager);
    }

    this.client = clientBuilder.build();
    this.client.start();
  }

  private HttpAsyncClientBuilder setProxy(HttpAsyncClientBuilder builder, Proxy proxy) {
    if (proxy != null) {
      if (proxy.type() != Proxy.Type.HTTP) {
        throw new IllegalArgumentException("Only HTTP proxies are supported, " + proxy.type()
            + " was provided");
      }

      if (proxy.address() instanceof InetSocketAddress) {
        InetSocketAddress inetAddress = (InetSocketAddress) proxy.address();
        URI uri;
        try {
          uri = new URI("http", null, inetAddress.getHostName(),
              inetAddress.getPort(), "", "", "");
        } catch (URISyntaxException e) {
          throw new RuntimeException("Unexpected error while building proxy URI", e);
        }

        return builder.setProxy(HttpHost.create(uri));
      } else {
        throw new IllegalArgumentException("Only InetSocketAddress proxy addresses are supported, "
            + proxy.address() + " was provided");
      }
    }

    return builder;
  }

  @Override
  public Publisher<AsyncHttpResponse> send(AsyncHttpRequest httpRequest) {
    ApacheRequestPublisher publisher = new ApacheRequestPublisher(client, httpRequest);

    return Utils.map(publisher, new Utils.Converter<SimpleHttpResponse, AsyncHttpResponse>() {
      @Override
      public AsyncHttpResponse convert(final SimpleHttpResponse from) {
        final LinkedHashMap<String, String> headers = new LinkedHashMap<>();

        for (Header header : from.getHeaders()) {
          headers.put(header.getName(), header.getValue());
        }

        return new ApacheAsyncHttpResponse(from.getCode(), headers, from.getBodyText());
      }
    });
  }

  @Override
  public void close(boolean wait) {
    CloseMode mode = wait ? CloseMode.GRACEFUL : CloseMode.IMMEDIATE;
    this.client.close(mode);
  }

  @Override
  public void close() {
    close(true);
  }

  private static class ApacheAsyncHttpResponse implements AsyncHttpResponse {
    private final int code;
    private final LinkedHashMap<String, String> headers;
    private final String body;

    ApacheAsyncHttpResponse(int code, LinkedHashMap<String, String> headers, String body) {
      this.code = code;
      this.headers = headers;
      this.body = body;
    }

    @Override
    public int getStatusCode() {
      return code;
    }

    @Override
    public Iterable<Map.Entry<String, String>> getHeaders() {
      return headers.entrySet();
    }

    @Override
    public String getBody() {
      return body;
    }
  }

  public static final class Builder {
    private Proxy proxy;
    private AsyncClientConnectionManager connectionManager;

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
     * The {@link AsyncClientConnectionManager} used to establish HTTP connections.
     *
     * @param connectionManager the connection manager
     * @return the builder instance.
     */
    public Builder connectionManager(AsyncClientConnectionManager connectionManager) {
      this.connectionManager = connectionManager;
      return this;
    }

    /**
     * Builds the {@link ApacheAsyncHttpClient} HTTP client.
     *
     * @return the HTTP client.
     */
    public ApacheAsyncHttpClient build() {
      return new ApacheAsyncHttpClient(this);
    }
  }
}
