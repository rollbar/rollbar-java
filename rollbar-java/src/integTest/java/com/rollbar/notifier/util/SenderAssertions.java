package com.rollbar.notifier.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.exception.ApiException;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.sender.result.Result;

public final class SenderAssertions {

  private SenderAssertions() {}

  public static final SenderListener assertResponseSuccess(String uui) {
    return new SuccessAssertion(uui);
  }

  public static final SenderListener assertApiError(int responseCode, String apiError) {
    Result result = new Result.Builder()
        .body(apiError)
        .code(1)
        .build();
    Response response = new Response.Builder()
        .status(responseCode)
        .result(result)
        .build();
    return new ErrorAssertion(new ApiException(response));
  }

  private static class SuccessAssertion implements SenderListener {

    private final String uuid;

    public SuccessAssertion(String uuid) {
      this.uuid = uuid;
    }

    @Override
    public void onResponse(Payload payload, Response response) {
      assertFalse(response.getResult().isError());
      assertThat(response.getResult().getContent(), is(uuid));
    }

    @Override
    public void onError(Payload payload, Exception error) {
      fail("Should not be called onError: " + error.getMessage());
    }
  }

  private static class ErrorAssertion implements SenderListener {

    private final Throwable error;

    public ErrorAssertion(Exception error) {
      this.error = error;
    }

    @Override
    public void onResponse(Payload payload, Response response) {
      fail("Should not be called onResponse.");
    }

    @Override
    public void onError(Payload payload, Exception error) {
      assertThat(error, instanceOf(SenderException.class));
      assertThat(error.getCause(), is(this.error));
    }
  }
}
