package com.rollbar.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;

class TelemetryNavigationCallbacks implements Application.ActivityLifecycleCallbacks {
    private final TelemetryEventTracker telemetryEventTracker;
    private String lastActivity;

    TelemetryNavigationCallbacks(TelemetryEventTracker telemetryEventTracker) {
        this.telemetryEventTracker = telemetryEventTracker;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        String current = activity.getClass().getSimpleName();

        if (lastActivity != null && !lastActivity.equals(current)) {
            telemetryEventTracker.recordNavigationEventFor(
                Level.INFO,
                Source.CLIENT,
                lastActivity,
                current
            );
        }

        lastActivity = current;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
