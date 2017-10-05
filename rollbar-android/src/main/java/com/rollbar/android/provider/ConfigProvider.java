package com.rollbar.android.provider;

import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;

public interface ConfigProvider {
    /**
     * Provides the config given a builder.
     *
     * @param builder an instance of {@link ConfigBuilder} with defaults set
     * @return the config
     */
    Config provide(ConfigBuilder builder);
}
