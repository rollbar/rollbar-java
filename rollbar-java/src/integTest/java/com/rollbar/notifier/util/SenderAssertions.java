package com.rollbar.notifier.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.exception.ApiException;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.sender.result.Result;

public final class SenderAssertions {

  private SenderAssertions() {}

  public static SuccessAssertion assertResponseSuccess(String uui) {
    return new SuccessAssertion(Thread.currentThread(), uui);
  }

  public static ErrorAssertion assertApiError(int responseCode, String apiError) {
    Result result = new Result.Builder()
        .body(apiError)
        .code(1)
        .build();
    Response response = new Response.Builder()
        .status(responseCode)
        .result(result)
        .build();
    return new ErrorAssertion(Thread.currentThread(), new ApiException(response));
  }

  public static ErrorAssertion assertSenderError(Class<? extends Exception> cause) {
    return new ErrorAssertion(Thread.currentThread(), cause);
  }

  public static class SuccessAssertion extends AssertListener {
    private final String uuid;

    public SuccessAssertion(Thread testThread, String uuid) {
      super(testThread);
      this.uuid = uuid;
    }

    @Override
    public void onResponseAssert(Payload payload, Response response) {
      assertFalse(response.getResult().isError());
      assertThat(response.getResult().getContent(), is(uuid));
    }

    @Override
    public void onErrorAssert(Payload payload, Exception error) {
      fail("Should not be called onError: " + error.getMessage());
    }
  }

  public static class ErrorAssertion extends AssertListener {
    private final Throwable error;
    private final Class<? extends Exception> errorClass;

    public ErrorAssertion(Thread testThread, Exception error) {
      this(testThread, error, null);
    }

    public ErrorAssertion(Thread testThread, Class<? extends Exception> causeClass) {
      this(testThread, null, causeClass);
    }

    private ErrorAssertion(Thread testThread, Exception error,
                           Class<? extends Exception> causeClass) {
      super(testThread);
      this.error = error;
      this.errorClass = causeClass;
    }

    @Override
    public void onResponseAssert(Payload payload, Response response) {
      fail("Should not be called onResponse.");
    }

    @Override
    public void onErrorAssert(Payload payload, Exception error) {
      assertThat(error, instanceOf(SenderException.class));
      if (this.error != null) {
        assertThat(error.getCause(), is(this.error));
      }
      if (this.errorClass != null) {
        assertThat(error.getCause(), instanceOf(this.errorClass));
      }
    }
  }

  private static abstract class AssertListener implements SenderListener {
    private volatile boolean called;
    private final Thread testThread;
    private Runnable deferredAsserts;

    private AssertListener(Thread testThread) {
      this.testThread = testThread;
      this.called = false;
      this.deferredAsserts = () -> {
      };
    }

    public void assertCalled() {
      deferredAsserts.run();
      assertTrue("Listener was not called", this.called);
    }

    @Override
    public void onResponse(Payload payload, Response response) {
      this.called = true;
      if (Thread.currentThread().equals(testThread)) {
        onResponseAssert(payload, response);
      } else {
        Runnable current = deferredAsserts;
        deferredAsserts = () -> {
          current.run();
          onResponseAssert(payload, response);
        };
      }
    }

    @Override
    public void onError(Payload payload, Exception error) {
      this.called = true;
      if (Thread.currentThread().equals(testThread)) {
        onErrorAssert(payload, error);
      } else {
        Runnable current = deferredAsserts;
        deferredAsserts = () -> {
          current.run();
          onErrorAssert(payload, error);
        };
      }
    }

    public abstract void onResponseAssert(Payload payload, Response response);

    public abstract void onErrorAssert(Payload payload, Exception error);
  }
}
