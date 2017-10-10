package com.rollbar.android.provider;

import com.rollbar.api.payload.data.Notifier;
import com.rollbar.notifier.provider.Provider;

/**
 * Android implementation to provide the {@link Notifier}.
 */
public class NotifierProvider implements Provider<Notifier> {

    private final Notifier notifier;

    public NotifierProvider(String version) {
        this.notifier = new Notifier.Builder()
                .name("rollbar-android")
                .version(version)
                .build();
    }

    @Override
    public Notifier provide() {
        return notifier;
    }
}