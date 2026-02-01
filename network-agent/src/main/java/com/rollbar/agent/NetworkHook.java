package com.rollbar.agent;

import java.net.URL;

public class NetworkHook {

  private static final ThreadLocal<Context> CTX =
    ThreadLocal.withInitial(Context::new);

  public static void onConnect(URL url, String method, long startNanos) {
    Context ctx = CTX.get();
    ctx.url = url;
    ctx.method = method;
    ctx.startNanos = startNanos;
  }

  public static void onResponse(int status) {
    Context ctx = CTX.get();
    long durationMs =
      (System.nanoTime() - ctx.startNanos) / 1_000_000;

    System.out.println(
      "[HTTP] " + ctx.method + " " + ctx.url +
        " -> " + status +
        " (" + durationMs + "ms)"
    );

    CTX.remove();
  }

  private static class Context {
    URL url;
    String method;
    long startNanos;
  }
}
