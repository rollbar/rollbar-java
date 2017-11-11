package com.rollbar.web.example.server;

import com.rollbar.api.payload.data.Server;
import com.rollbar.notifier.provider.Provider;

public class ServerProvider implements Provider<Server> {


  @Override
  public Server provide() {
    return new Server.Builder()
        .codeVersion("1.0.0")
        .branch("master")
        .host("localhost")
        .build();
  }
}
