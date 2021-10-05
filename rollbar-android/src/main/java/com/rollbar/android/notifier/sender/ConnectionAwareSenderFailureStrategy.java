package com.rollbar.android.notifier.sender;

import android.content.Context;
import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.SenderFailureStrategy;
import com.rollbar.notifier.sender.result.Response;
import com.rollbar.notifier.sender.result.Result;
import com.rollbar.notifier.util.ObjectsUtils;

import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * <p>
 * This class will monitor the failures that occur when sending occurrences, and if it determines
 * that the errors are caused by network connection issues, it will temporarily suspend sending
 * occurrences, and mark the payloads that failed for retry.
 * </p>
 * <p>
 *     It uses Android's ConnectivityManager, so for full functionality the application should
 *     be granted the android.permission.ACCESS_NETWORK_STATE permission.
 * </p>
 * <p>
 *     When that permission is available and this class detects the network is down, sending
 *     occurrences will be suspended for up to 3 minutes, and immediately resumes if it receives an
 *     OS notification indicating that the network is available again.
 * </p>
 * <p>
 *     If the permission is not available, this class will attempt to detect network issues based
 *     on the type of exception thrown, and it will briefly suspend sending occurrences for
 *     1 second if it suspects network availability might be the root cause of the failures.
 * </p>
 */
public class ConnectionAwareSenderFailureStrategy implements SenderFailureStrategy {
  private final ConnectivityDetector detector;
  private final NanoTimeProvider timeProvider;
  private volatile boolean isSendingSuspended;
  private volatile long resumeTime;

  // When the network is unavailable, we will stop sending occurrences for up to 5 minutes, or
  // until our connectivity network receiver is notified that the network is back. We don't want
  // to wait for the connectivity receiver indefinitely because it's possible for it not be called
  // under some error scenarios.
  private static final long SUSPEND_TIME_NO_NETWORK_MS = 300000;

  // If there is a network issue that appears to be network connectivity related, but we cannot
  // confirm it via the ConnectivityManager, we will suspend sending occurrences for a second.
  private static final long SUSPEND_TIME_UNKNOWN_ISSUE_MS = 1000;

  ConnectionAwareSenderFailureStrategy(ConnectivityDetector detector,
                                       NanoTimeProvider timeProvider) {
    ObjectsUtils.requireNonNull(detector, "detector cannot be null");
    ObjectsUtils.requireNonNull(timeProvider, "timeProvider cannot be null");

    this.detector = detector;
    this.timeProvider = timeProvider;
    this.detector.setNetworkRestoredSignal(new Runnable() {
      @Override
      public void run() {
        resume();
      }
    });

    this.isSendingSuspended = false;
  }

  public ConnectionAwareSenderFailureStrategy(Context androidContext) {
    this(new ConnectivityDetector(androidContext), new SystemNanoTimeProvider());
  }

  public void updateContext(Context androidContext) {
    this.detector.updateContext(androidContext);
  }

  @Override
  public PayloadAction getAction(Payload payload, Response response) {
    NetworkStatus status = getNetworkStatus(response);
    return getAction(status);
  }

  @Override
  public PayloadAction getAction(Payload payload, Exception error) {
    NetworkStatus status = getNetworkStatus(error);
    return getAction(status);
  }

  @Override
  public boolean isSendingSuspended() {
    if (isSendingSuspended) {
      if (timeProvider.nanoTime() >= resumeTime) {
        resume();
      }
    }

    return isSendingSuspended;
  }

  @Override
  public void close() {
    this.detector.close();
  }

  private PayloadAction getAction(NetworkStatus status) {
    switch (status) {
      case OK:
        return PayloadAction.NONE;
      case NO_NETWORK:
        suspend(SUSPEND_TIME_NO_NETWORK_MS);
        return PayloadAction.CAN_BE_RETRIED;
      case UNKNOWN_NETWORK_ISSUE:
        suspend(SUSPEND_TIME_UNKNOWN_ISSUE_MS);
        return PayloadAction.CAN_BE_RETRIED;
    }

    return PayloadAction.NONE;
  }

  private void suspend(long suspendTimeMillis) {
    isSendingSuspended = true;
    resumeTime = timeProvider.nanoTime() + (suspendTimeMillis * 1000000L);
  }

  private void resume() {
    isSendingSuspended = false;
  }

  private NetworkStatus getNetworkStatus(Response response) {
    if (isSuccessfulResult(response.getResult())) {
      return NetworkStatus.OK;
    }
    if (isSuspiciousHTTPStatus(response.getStatus())) {
      return detector.isNetworkAvailable() ? NetworkStatus.OK : NetworkStatus.NO_NETWORK;
    }

    return NetworkStatus.OK;
  }

  private boolean isSuccessfulResult(Result result) {
    if (result == null) {
      return false;
    }

    return !result.isError();
  }

  private NetworkStatus getNetworkStatus(Throwable error) {
    if (error == null) {
      return NetworkStatus.OK;
    }

    if (error instanceof SocketException || error instanceof UnknownHostException) {
      if (detector.isNetworkAvailable()) {
        return NetworkStatus.UNKNOWN_NETWORK_ISSUE;
      } else {
        return NetworkStatus.NO_NETWORK;
      }
    } else if (error.getCause() != null) {
      return getNetworkStatus(error.getCause());
    }

    return NetworkStatus.OK;
  }

  private boolean isSuspiciousHTTPStatus(int status) {
    // This could be a captive portal. Some buggy portals use 301 and 308, so we check for those
    // as well.
    return status == 301 || status == 302 || status == 307 || status == 308;
  }

  private enum NetworkStatus {
    OK,
    NO_NETWORK,
    UNKNOWN_NETWORK_ISSUE
  }

  interface NanoTimeProvider {
    long nanoTime();
  }

  static class SystemNanoTimeProvider implements NanoTimeProvider {
    @Override
    public long nanoTime() {
      return System.nanoTime();
    }
  }
}
