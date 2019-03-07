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
    private final int captureIp;
    private final int maxLogcatSize;

    private static final String CAPTURE_IP_ANONYMIZE = "anonymize";
    private static final String CAPTURE_IP_NONE = "none";
    private static final int CAPTURE_IP_TYPE_FULL = 0;
    private static final int CAPTURE_IP_TYPE_ANONYMIZE = 1;
    private static final int CAPTURE_IP_TYPE_NONE = 2;

    /**
     * Constructor.
     */
    ClientProvider(Builder builder) {
        this.versionCode = builder.versionCode;
        this.versionName = builder.versionName;
        this.includeLogcat = builder.includeLogcat;
        if (builder.captureIp != null) {
          if (builder.captureIp.equals(CAPTURE_IP_ANONYMIZE)) {
            this.captureIp = CAPTURE_IP_TYPE_ANONYMIZE;
          } else if (builder.captureIp.equals(CAPTURE_IP_NONE)) {
            this.captureIp = CAPTURE_IP_TYPE_NONE;
          } else {
            this.captureIp = CAPTURE_IP_TYPE_FULL;
          }
        } else {
          this.captureIp = CAPTURE_IP_TYPE_FULL;
        }
        this.maxLogcatSize = builder.maxLogcatSize;
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

        Client.Builder clientBuilder = new Client.Builder()
                .addClient("android", androidData)
                .addTopLevel("code_version", this.versionCode)
                .addTopLevel("name_version", this.versionName)
                .addTopLevel("version_code", this.versionCode)
                .addTopLevel("version_name", this.versionName)
                .addTopLevel("timestamp", System.currentTimeMillis() / 1000);

        if (this.captureIp == CAPTURE_IP_TYPE_FULL) {
            clientBuilder.addTopLevel("user_ip", "$remote_ip");
        } else if (this.captureIp == CAPTURE_IP_TYPE_ANONYMIZE) {
            clientBuilder.addTopLevel("user_ip", "$remote_ip_anonymize");
        }
        return clientBuilder.build();
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
                    if (lines.size() > this.maxLogcatSize) {
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
        private String captureIp;
        private int maxLogcatSize;

        /**
         * Constructor.
         */
        public Builder() {
            this.maxLogcatSize = 100;
        }

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
         * How to capture the remote client ip, either completely, anonymized, or not at all.
         * @param captureIp one of: full, anonymize, none.
         * @return the builder instance.
         */
        public Builder captureIp(String captureIp) {
            this.captureIp = captureIp;
            return this;
        }

        /**
         * The maximum number of logcat lines to capture if logcat capturing is on.
         * @param maxLogcatSize the max number of lines to capture
         * @return the builder instance
         */
        public Builder maxLogcatSize(int maxLogcatSize) {
            if (maxLogcatSize >= 0) {
                this.maxLogcatSize = maxLogcatSize;
            }
            return this;
        }

        /**
         * Builds the {@link ClientProvider client provider}.
         * @return the client provider.
         */
        public ClientProvider build() { return new ClientProvider(this); }
    }
}
