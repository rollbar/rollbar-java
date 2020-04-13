package com.rollbar.android.provider;

import static com.rollbar.android.util.Constants.ROLLBAR_NAMESPACE;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.rollbar.api.payload.data.Notifier;
import com.rollbar.notifier.provider.Provider;

/**
 * Android implementation to provide the {@link Notifier}.
 */
public class NotifierProvider implements Provider<Notifier> {

    private static final String VERSION_METADATA_NAME = ROLLBAR_NAMESPACE + "._notifier.version";

    private static final String NAME = "rollbar-android";

    private final Notifier notifier;

    public NotifierProvider(Context context) {
        this(loadVersionFromContext(context));
    }

    public NotifierProvider(String version) {
        this.notifier = new Notifier.Builder()
                .name(NAME)
                .version(version)
                .build();
    }

    public NotifierProvider(String version, String name) {
        this.notifier = new Notifier.Builder()
                .name(name)
                .version(version)
                .build();
    }

    @Override
    public Notifier provide() {
        return notifier;
    }

    private static String loadVersionFromContext(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle data = ai.metaData;
            return data.getString(VERSION_METADATA_NAME);
        } catch(PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }
}
