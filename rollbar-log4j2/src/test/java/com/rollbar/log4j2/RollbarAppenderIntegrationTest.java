package com.rollbar.log4j2;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;
import com.rollbar.notifier.sender.Sender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RollbarAppenderIntegrationTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Sender sender;

    @After
    public void tearDown() {
        TestConfigProvider.SENDER.set(null);
    }

    @Test
    public void whenConfigProviderIsProvidedIsShouldNotRequireATokenParameter() throws IOException {
        TestConfigProvider.SENDER.set(sender);
        String configClass = TestConfigProvider.class.getName();

        String appenderConfig = "<configProviderClassName>" + configClass + "</configProviderClassName>\n";

        LoggerContext context = getLoggerContext(appenderConfig);

        context.getRootLogger().error("Test error");

        assertTrue("Timed out shutting down the logger context", context.stop(1, TimeUnit.MINUTES));
        
        ArgumentCaptor<Payload> actualPayload = ArgumentCaptor.forClass(Payload.class);
        verify(sender, times(1)).send(actualPayload.capture());

        assertThat(actualPayload.getValue().getAccessToken(), equalTo(TestConfigProvider.TEST_TOKEN));
    }

    @Test
    public void whenTokenIsProvidedIsShouldNotRequireAConfigProvider() throws IOException {
        String token = "1234";

        String appenderConfig = "<accessToken>" + token + "</accessToken>\n";

        LoggerContext context = getLoggerContext(appenderConfig);

        // The sender has been built using the default config so we can't easily mock it. We will just check that
        // the appender has been created.
        assertThat(context.getRootLogger().getAppenders().keySet(), contains("ROLLBAR"));

        assertTrue("Timed out shutting down the logger context", context.stop(1, TimeUnit.MINUTES));
    }

    @Test
    public void whenTokenAndConfigProviderAreMissingItShouldFailToInitializeAppender() throws IOException {
        String token = "1234";

        String appenderConfig = "<environment>development</environment>\n";

        LoggerContext context = getLoggerContext(appenderConfig);

        // log4j will swallow all appender exceptions so we can't check them directly.
        assertThat(context.getRootLogger().getAppenders().keySet(), not(contains("ROLLBAR")));

        assertTrue("Timed out shutting down the logger context", context.stop(1, TimeUnit.MINUTES));
    }

    private LoggerContext getLoggerContext(String appenderConfig) throws IOException {
        /*
        There is a slightly more unit-level test we could do here with something like this:

        PluginManager plugins = new PluginManager("core");
        plugins.collectPlugins(Collections.singletonList("com.rollbar"));
        PluginType<?> plugin = plugins.getPluginType("Rollbar");
        DefaultConfiguration pluginConfig = new DefaultConfiguration();

        (setup config)

        RollbarAppender appender = (RollbarAppender) new PluginBuilder(plugin)...

        ...but it depends on log4j plugin classes that, while public, are probably not as stable as the standard
        logging context initialization classes. So better do a full on integration test. Execution time is fast enough,
        ~50ms per test
         */

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Configuration status=\"INFO\">\n" +
                "  <Appenders>\n" +
                "    <Rollbar name=\"ROLLBAR\">" + appenderConfig + "</Rollbar>\n" +
                "  </Appenders>\n" +
                "  <Loggers>\n" +
                "    <Root level=\"debug\">\n" +
                "      <AppenderRef ref=\"ROLLBAR\" />\n" +
                "    </Root>\n" +
                "  </Loggers>\n" +
                "</Configuration>";

        ConfigurationSource config = new ConfigurationSource(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        return new Log4jContextFactory().getContext(this.getClass().getName(),
                RollbarAppender.class.getClassLoader(), null, false, config);
    }

    public static class TestConfigProvider implements ConfigProvider {
        public static final ThreadLocal<Sender> SENDER = new ThreadLocal<>();
        public static final String TEST_TOKEN = "CONFIG_PROVIDER_TOKEN";

        @Override
        public Config provide(ConfigBuilder builder) {
            return builder.accessToken(TEST_TOKEN).sender(SENDER.get()).build();
        }
    }
}
