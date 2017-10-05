package com.rollbar.android.provider;

import android.util.Log;
import com.rollbar.android.Rollbar;
import com.rollbar.api.payload.data.Client;
import com.rollbar.notifier.provider.Provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientProvider implements Provider<Client> {

    private final int versionCode;
    private final String versionName;
    private boolean includeLogcat;

    private static final int MAX_LOGCAT_SIZE = 100;

    /**
     * Constructor.
     */
    ClientProvider(Builder builder) {
        this.versionCode = builder.versionCode;
        this.versionName = builder.versionName;
        this.includeLogcat = builder.includeLogcat;
    }

    @Override
    public Client provide() {
        Map<String, Object>  androidData = new HashMap<>();
        androidData.put("phone_model", android.os.Build.MODEL);
        androidData.put("android_version", android.os.Build.VERSION.RELEASE);
        // NOTE use versionName instead of versionCode in order to fix
        //      'Resolving Rollbar Items in Versions' feature https://rollbar.com/blog/post/2013/09/17/resolving-rollbar-items-in-versions
        androidData.put("code_version", this.versionName);
        // NOTE keep original versionCode value with different name
        androidData.put("version_code", this.versionCode);
        androidData.put("version_name", this.versionName);

        if (includeLogcat) {
            androidData.put("logs", getLogcatInfo());
        }

        return new Client.Builder()
                .addClient("android", androidData)
                .addTopLevel("code_version", this.versionCode)
                .addTopLevel("name_version", this.versionName)
                .addTopLevel("version_code", this.versionCode)
                .addTopLevel("version_name", this.versionName)
                .addTopLevel("user_ip", "$remote_ip")
                .addTopLevel("timestamp", System.currentTimeMillis() / 1000)
                .build();
    }

    public void setIncludeLogcat(boolean includeLogcat) {
        this.includeLogcat = includeLogcat;
    }

    private ArrayList<String> getLogcatInfo() {
        ArrayList<String> log = null;
        int pid = android.os.Process.myPid();
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(isr, 8192);
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(String.valueOf(pid))) {
                    lines.add(line);
                    if (lines.size() > MAX_LOGCAT_SIZE) {
                        lines.remove(0);
                    }
                }
            }
            log = new ArrayList<>(lines);
        } catch (IOException e) {
            Log.e(Rollbar.TAG, "Unable to collect logcat info.", e);
        }
        return log;
    }

    /**
     * Builder class for {@link ClientProvider}
     */
    public static final class Builder {
        private int versionCode;
        private String versionName;
        private boolean includeLogcat;

        /**
         * The Android version code from the context
         * @param versionCode the version code
         * @return the builder instance
         */
        public Builder versionCode(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        /**
         * The Android version name from the context
         * @param versionName the version name
         * @return the builder instance
         */
        public Builder versionName(String versionName) {
            this.versionName = versionName;
            return this;
        }

        /**
         * Whether or not to include logcat log lines
         * @param includeLogcat the version name
         * @return the builder instance
         */
        public Builder includeLogcat(boolean includeLogcat) {
            this.includeLogcat = includeLogcat;
            return this;
        }

        /**
         * Builds the {@link ClientProvider client provider}.
         * @return the client provider.
         */
        public ClientProvider build() { return new ClientProvider(this); }
    }
}
