package com.rollbar.notifier;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import static org.mockito.Mockito.verify;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Source;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.telemetry.TelemetryEventTracker;
import com.rollbar.notifier.util.BodyFactory;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RollbarBaseTest {
  private final Level level = Level.DEBUG;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private TelemetryEventTracker telemetryEventTracker;

  @Mock
  private BodyFactory dummyFactory;

  @Test
  public void shouldRecordALogEventWithServerSourceWhenThePlatformIsNotAndroid() {
    String message = "message";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith("spring"), dummyFactory, null);

    sut.recordLogEventFor(level, message);

    verify(telemetryEventTracker).recordLogEventFor(level, Source.SERVER, message);
  }

  @Test
  public void shouldRecordALogEventWithClientSourceWhenThePlatformIsAndroid() {
    String message = "message";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith("android"), dummyFactory, null);

    sut.recordLogEventFor(level, message);

    verify(telemetryEventTracker).recordLogEventFor(level, Source.CLIENT, message);
  }

  @Test
  public void shouldRecordAManualEventWithServerSourceWhenThePlatformIsNotAndroid() {
    String message = "message";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith(null), dummyFactory, null);

    sut.recordManualEventFor(level, message);

    verify(telemetryEventTracker).recordManualEventFor(level, Source.SERVER, message);
  }

  @Test
  public void shouldRecordAManualEventWithClientSourceWhenThePlatformIsAndroid() {
    String message = "message";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith("android"), dummyFactory, null);

    sut.recordManualEventFor(level, message);

    verify(telemetryEventTracker).recordManualEventFor(level, Source.CLIENT, message);
  }

  @Test
  public void shouldRecordANetworkEventWithServerSourceWhenThePlatformIsNotAndroid() {
    String method = "method";
    String url = "url";
    String statusCode = "status code";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith("any"), dummyFactory, null);

    sut.recordNetworkEventFor(level, method, url, statusCode);

    verify(telemetryEventTracker).recordNetworkEventFor(level, Source.SERVER, method, url, statusCode);
  }

  @Test
  public void shouldRecordANetworkEventWithClientSourceWhenThePlatformIsAndroid() {
    String method = "method";
    String url = "url";
    String statusCode = "status code";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith("android"), dummyFactory, null);

    sut.recordNetworkEventFor(level, method, url, statusCode);

    verify(telemetryEventTracker).recordNetworkEventFor(level, Source.CLIENT, method, url, statusCode);
  }

  @Test
  public void shouldRecordANavigationEventWithServerSourceWhenThePlatformIsNotAndroid() {
    String from = "from";
    String to = "to";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith("any"), dummyFactory, null);

    sut.recordNavigationEventFor(level, from, to);

    verify(telemetryEventTracker).recordNavigationEventFor(level, Source.SERVER, from, to);
  }

  @Test
  public void shouldRecordANavigationEventWithClientSourceWhenThePlatformIsAndroid() {
    String from = "from";
    String to = "to";
    RollbarBase<Void, Config> sut = new RollbarBaseImpl(getConfigWith("android"), dummyFactory, null);

    sut.recordNavigationEventFor(level, from, to);

    verify(telemetryEventTracker).recordNavigationEventFor(level, Source.CLIENT, from, to);
  }

  private Config getConfigWith(String platform) {
    return withAccessToken("dummy token")
        .telemetryEventTracker(telemetryEventTracker)
        .platform(platform)
        .build();
  }

  static class RollbarBaseImpl extends RollbarBase<Void, Config> {

    protected RollbarBaseImpl(Config config, BodyFactory bodyFactory, Void emptyResult) {
      super(config, bodyFactory, emptyResult);
    }

    @Override
    protected Void sendPayload(Config config, Payload payload) {
      return null;
    }
  }
}
