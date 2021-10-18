package com.rollbar.android.notifier.sender;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.SenderFailureStrategy;
import com.rollbar.notifier.sender.result.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

import java.net.ConnectException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class ConnectionAwareSenderFailureStrategyTest {
  protected ConnectivityDetector connectivityDetector;
  protected ConnectionAwareSenderFailureStrategy sut;
  protected Runnable networkRestoredSignal;
  protected ConnectionAwareSenderFailureStrategy.NanoTimeProvider timeProvider;
  protected Payload payload;

  @Before
  public void setUp() {
    connectivityDetector = mock(ConnectivityDetector.class);
    ArgumentCaptor<Runnable> signalCaptor = ArgumentCaptor.forClass(Runnable.class);

    timeProvider = mock(ConnectionAwareSenderFailureStrategy.NanoTimeProvider.class);
    when(timeProvider.nanoTime()).thenReturn(0L);

    sut = new ConnectionAwareSenderFailureStrategy(connectivityDetector, timeProvider);
    verify(connectivityDetector).setNetworkRestoredSignal(signalCaptor.capture());
    networkRestoredSignal = signalCaptor.getValue();
    payload = mock(Payload.class);
  }

  protected void moveTimeForward(long timeMs) {
    long current = timeProvider.nanoTime();
    reset(timeProvider);
    when(timeProvider.nanoTime()).thenReturn(current + timeMs * 1000000L);
  }

  public static class BaseTest extends ConnectionAwareSenderFailureStrategyTest {
    @Test
    public void shouldNotBeSuspendedAfterInitialization() {
      assertThat(sut.isSendingSuspended(), is(false));
    }

    @Test
    public void whenNetworkUpSignalIsCalledSendingShouldResume() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(false);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload,
          new ConnectException("No connection"));

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.CAN_BE_RETRIED));
      assertThat(sut.isSendingSuspended(), is(true));

      networkRestoredSignal.run();

      assertThat(sut.isSendingSuspended(), is(false));
    }
  }

  public static class GivenANetworkErrorTest extends ConnectionAwareSenderFailureStrategyTest {
    private final ConnectException exception = new ConnectException("No connection");

    @Test
    public void ifTheNetworkIsDownSendingShouldBeSuspendedFor5MinutesAndPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(false);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, exception);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.CAN_BE_RETRIED));
      assertThat(sut.isSendingSuspended(), is(true));

      moveTimeForward(299999);

      assertThat(sut.isSendingSuspended(), is(true));

      moveTimeForward(1);

      assertThat(sut.isSendingSuspended(), is(false));
    }

    @Test
    public void ifTheNetworkIsUpSendingShouldBeSuspendedFor1SecAndPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(true);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, exception);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.CAN_BE_RETRIED));
      assertThat(sut.isSendingSuspended(), is(true));

      moveTimeForward(999);

      assertThat(sut.isSendingSuspended(), is(true));

      moveTimeForward(1);

      assertThat(sut.isSendingSuspended(), is(false));
    }
  }

  public static class GivenAnUnrecognizedError extends ConnectionAwareSenderFailureStrategyTest {
    private final IllegalStateException exception = new IllegalStateException("Something unexpected");

    @Test
    public void ifTheNetworkIsDownSendingNotNotBeSuspendedNorPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(false);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, exception);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.NONE));
      assertThat(sut.isSendingSuspended(), is(false));
    }

    @Test
    public void ifTheNetworkIsUpSendingNotNotBeSuspendedNorPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(true);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, exception);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.NONE));
      assertThat(sut.isSendingSuspended(), is(false));
    }
  }

  @RunWith(Parameterized.class)
  public static class GivenASuspiciousHTTPResponse extends ConnectionAwareSenderFailureStrategyTest {
    private final Response response;

    @Parameterized.Parameters(name = "HTTP Status: {0}")
    public static Object[][] parameters() {
      int[] statuses = {301, 302, 307, 308};
      Object[][] params = new Object[statuses.length][];

      for (int j = 0; j < params.length; ++j) {
        params[j] = new Object[]{statuses[j]};
      }

      return params;
    }

    public GivenASuspiciousHTTPResponse(int status) {
      this.response = new Response.Builder().status(status).build();
    }

    @Test
    public void ifTheNetworkIsDownSendingShouldBeSuspendedAndPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(false);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, response);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.CAN_BE_RETRIED));
      assertThat(sut.isSendingSuspended(), is(true));

      moveTimeForward(299999);

      assertThat(sut.isSendingSuspended(), is(true));

      moveTimeForward(1);

      assertThat(sut.isSendingSuspended(), is(false));
    }

    @Test
    public void ifTheNetworkIsUpSendingShouldNotBeSuspendedNorPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(true);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, response);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.NONE));
      assertThat(sut.isSendingSuspended(), is(false));
    }
  }

  @RunWith(Parameterized.class)
  public static class GivenAnUnexpectedResponse extends ConnectionAwareSenderFailureStrategyTest {
    private final Response response;

    @Parameterized.Parameters(name = "HTTP Status: {0}")
    public static Object[][] parameters() {
      int[] statuses = {400, 401, 402, 403, 403, 404, 404, 405, 406, 407, 408, 409, 410, 411,
          412, 413, 414, 415, 416, 417, 422, 423, 424, 426, 500, 501, 502, 503, 504, 505,
          506, 507, 508, 510, 511, 300, 303, 304};
      Object[][] params = new Object[statuses.length][];

      for (int j = 0; j < params.length; ++j) {
        params[j] = new Object[]{statuses[j]};
      }

      return params;
    }

    public GivenAnUnexpectedResponse(int status) {
      this.response = new Response.Builder().status(status).build();
    }

    @Test
    public void ifTheNetworkIsDownSendingShouldNotBeSuspendedNorPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(false);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, response);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.NONE));
      assertThat(sut.isSendingSuspended(), is(false));
    }

    @Test
    public void ifTheNetworkIsUpSendingShouldNotBeSuspendedNorPayloadRetried() {
      when(connectivityDetector.isNetworkAvailable()).thenReturn(true);

      SenderFailureStrategy.PayloadAction action = sut.getAction(payload, response);

      assertThat(action, equalTo(SenderFailureStrategy.PayloadAction.NONE));
      assertThat(sut.isSendingSuspended(), is(false));
    }
  }
}
