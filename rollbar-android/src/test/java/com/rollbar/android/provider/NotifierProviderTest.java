package com.rollbar.android.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.rollbar.api.payload.data.Notifier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class NotifierProviderTest {

    private static final String NAME = "rollbar-android";

    private static final String VERSION = "1.7.0";

    private static final String PACKAGE_NAME = "package_name";

    @Mock
    Context context;

    @Mock
    PackageManager packageManager;

    @Mock
    ApplicationInfo applicationInfo;

    @Mock
    Bundle bundle;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(bundle.getString(eq("com.rollbar.android._notifier.version")))
                .thenReturn(VERSION);
        applicationInfo.metaData = this.bundle;

        when(packageManager.getApplicationInfo(eq(PACKAGE_NAME), eq(PackageManager.GET_META_DATA)))
                .thenReturn(applicationInfo);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
    }

    @Test
    public void shouldUseVersionFromMetadata() {
        NotifierProvider sut = new NotifierProvider(context);

        Notifier notifier = sut.provide();

        assertEquals(VERSION, notifier.getVersion());
        assertEquals(NAME, notifier.getName());
    }
}
