package com.rollbar.android.notifier.sender;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import com.rollbar.notifier.util.ObjectsUtils;

import java.io.Closeable;

class ConnectivityDetector implements Closeable {
  private final Object receiverLock = new Object();
  private boolean hasNetworkStatePermission;
  private volatile NetworkUpBroadcastReceiver receiver;
  private Context androidContext;
  private Runnable networkRestoredSignal;

  ConnectivityDetector(Context androidContext) {
    this.updateContext(androidContext);
  }

  public void setNetworkRestoredSignal(Runnable networkRestoredSignal) {
    this.networkRestoredSignal = networkRestoredSignal;
  }

  public void updateContext(Context androidContext) {
    ObjectsUtils.requireNonNull(androidContext, "androidContext cannot be null");

    // Unregister from the old context, if there is one
    unregisterNetworkStateReceiver();

    this.androidContext = androidContext;
    this.hasNetworkStatePermission = hasNetworkStatePermission();
    if (this.hasNetworkStatePermission) {
      this.registerNetworkStateReceiver();
    } else {
      String message = "This application is missing the " +
              "android.permission.ACCESS_NETWORK_STATE permission. The Rollbar notifier " +
              "will *not* be able to detect when the network is unavailable.";
      Log.w(ConnectivityDetector.class.getCanonicalName(), message);
    }
  }

  @Override
  public void close() {
    unregisterNetworkStateReceiver();
  }

  boolean isNetworkAvailable() {
    if (!hasNetworkStatePermission) {
      // Assume network is up
      return true;
    }

    ConnectivityManager connectivityManager =
            (ConnectivityManager) androidContext.getSystemService(Context.CONNECTIVITY_SERVICE);

    if (connectivityManager == null) {
      // We don't know, assume connection is available.
      return true;
    }

    // We programmatically check for this permission in the constructor, and return early
    // in this method if we don't have it.
    @SuppressLint("MissingPermission")
    NetworkInfo network = connectivityManager.getActiveNetworkInfo();
    if (network == null) {
      return false;
    }

    return network.isAvailable() && network.isConnected();
  }

  private boolean hasNetworkStatePermission() {
    String permission = Manifest.permission.ACCESS_NETWORK_STATE;
    int res = androidContext.checkPermission(permission,
            android.os.Process.myPid(), android.os.Process.myUid());
    return res == PackageManager.PERMISSION_GRANTED;
  }

  private void registerNetworkStateReceiver() {
    synchronized (this.receiverLock) {
      unregisterNetworkStateReceiver();
      this.receiver = new NetworkUpBroadcastReceiver();
    }

    this.androidContext.registerReceiver(this.receiver,
            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    if (this.androidContext instanceof Activity) {
      // Allows examples and documentation to remain unmodified even if users enable the
      // connection state monitoring feature, otherwise all SDK users would need to add
      // logic to their `onDestroy` methods to stop the IntentReceiver from leaking.
      Activity targetActivity = (Activity) this.androidContext;
      if (targetActivity.getApplication() != null) {
        ReceiverActivityCallback activityCallback =
                new ReceiverActivityCallback(targetActivity);
        targetActivity.getApplication()
                .registerActivityLifecycleCallbacks(activityCallback);
      }
    }
  }

  private void unregisterNetworkStateReceiver() {
    synchronized (this.receiverLock) {
      if (this.receiver != null && this.androidContext != null) {
        this.androidContext.unregisterReceiver(this.receiver);
        this.receiver = null;
      }
    }
  }

  private class NetworkUpBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (isNetworkAvailable()) {
        Runnable signal = networkRestoredSignal;
        if (signal != null) {
          signal.run();
        }
      }
      // We resume based on a connection event, but we don't suspend based on it. We err on
      // the side of trying to send the occurrences even if we suspect the network is down,
      // since connectivity detection involves heuristics that aren't 100% reliable (eg. some
      // test endpoint such as http://clients3.google.com/generate_204 might be temporarily
      // unavailable or blocked, but https://api.rollbar.com might be working.)
      // We only suspend sending occurrences when a send attempt to the configured Rollbar
      // endpoint fails due to a network availability issue.
    }
  }

  private class ReceiverActivityCallback
          implements Application.ActivityLifecycleCallbacks {
    private final Activity targetActivity;

    public ReceiverActivityCallback(Activity targetActivity) {
      this.targetActivity = targetActivity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      // If the activity that created us is getting destroyed, free resources, as per the
      // official Android docs recommendation.
      if (activity == targetActivity) {
        unregisterNetworkStateReceiver();
      }
    }
  }
}
