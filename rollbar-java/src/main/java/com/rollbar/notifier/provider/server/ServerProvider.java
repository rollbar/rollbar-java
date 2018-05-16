package com.rollbar.notifier.provider.server;

import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.provider.Provider;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Server provider that populates the server's hostname.
 */
public class ServerProvider implements Provider<Server> {
  @Override
  public Server provide() {
    try {
      InetAddress host = InetAddress.getLocalHost();
      return new Server.Builder()
              .host(host.getHostName())
              .build();
    } catch (UnknownHostException e) {
      return new Server.Builder()
              .host("localhost")
              .build();
    }
  }
}
