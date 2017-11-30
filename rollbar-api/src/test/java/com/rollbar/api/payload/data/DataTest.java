package com.rollbar.api.payload.data;

import static com.rollbar.test.Factory.data;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class DataTest {

  @Test
  public void shouldBeEqual() {
    Data data1 = data();
    Data data2 = data();

    assertThat(data2, is(data1));
  }

  @Test
  public void shouldReturnAsJson() {
    Data data = data();

    Map<String, Object> expected = new HashMap<>();

    if (data.getEnvironment() != null) {
      expected.put("environment", data.getEnvironment());
    }
    if (data.getBody() != null) {
      expected.put("body", data.getBody());
    }
    if (data.getLevel() != null) {
      expected.put("level", data.getLevel().asJson());
    }
    if (data.getTimestamp() != null) {
      double timestamp_secs = data.getTimestamp() / 1000.0;
      expected.put("timestamp", timestamp_secs);
    }
    if (data.getCodeVersion() != null) {
      expected.put("code_version", data.getCodeVersion());
    }
    if (data.getPlatform() != null) {
      expected.put("platform", data.getPlatform());
    }
    if (data.getLanguage() != null) {
      expected.put("language", data.getLanguage());
    }
    if (data.getFramework() != null) {
      expected.put("framework", data.getFramework());
    }
    if (data.getContext() != null) {
      expected.put("context", data.getContext());
    }
    if (data.getRequest() != null) {
      expected.put("request", data.getRequest());
    }
    if (data.getPerson() != null) {
      expected.put("person", data.getPerson());
    }
    if (data.getServer() != null) {
      expected.put("server", data.getServer());
    }
    if (data.getClient() != null) {
      expected.put("client", data.getClient());
    }
    if (data.getCustom() != null) {
      expected.put("custom", data.getCustom());
    }
    if (data.getFingerprint() != null) {
      expected.put("fingerprint", data.getFingerprint());
    }
    if (data.getTitle() != null) {
      expected.put("title", data.getTitle());
    }
    if (data.getUuid() != null) {
      expected.put("uuid", data.getUuid());
    }
    if (data.getNotifier() != null) {
      expected.put("notifier", data.getNotifier());
    }

    assertThat(data.asJson(), is(expected));
  }
}
