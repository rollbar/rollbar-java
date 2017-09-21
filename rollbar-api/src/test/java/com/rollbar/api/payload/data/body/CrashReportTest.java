package com.rollbar.api.payload.data.body;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.rollbar.test.Factory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class CrashReportTest {

  @Test
  public void shouldBeEqual() {
    CrashReport crashReport1 = Factory.crashReport();
    CrashReport crashReport2 = Factory.crashReport();

    assertThat(crashReport2, is(crashReport1));
  }

  @Test
  public void shouldReturnAsJson() {
    CrashReport crashReport = Factory.crashReport();

    assertThat(crashReport.getKeyName(), is("crash_report"));

    Map<String, Object> expected = new HashMap<>();
    expected.put("raw", crashReport.getRaw());

    assertThat(crashReport.asJson(), is(expected));
  }
}