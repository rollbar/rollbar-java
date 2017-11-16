package com.rollbar.notifier.util;

@SuppressWarnings("unused")
public class RollbarResponse {

  private final int err;

  private final RollbarResult result;

  private RollbarResponse(int err, RollbarResult result) {
    this.err = err;
    this.result = result;
  }

  public static RollbarResponse success(String uuid) {
    return new RollbarResponse(0, new RollbarResult(uuid, null));
  }

  public static RollbarResponse error(String error) {
    return new RollbarResponse(1, new RollbarResult(null, error));
  }

  private static class RollbarResult {

    private final String uuid;

    private final String message;

    public RollbarResult(String uuid, String message) {
      this.uuid = uuid;
      this.message = message;
    }
  }
}
