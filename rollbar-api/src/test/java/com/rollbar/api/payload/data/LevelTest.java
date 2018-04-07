package com.rollbar.api.payload.data;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LevelTest {

  @Parameter
  public String name;

  @Parameter(1)
  public Level expected;

  @Parameters(name = "{index}: name({0}) is level({1})")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { "critical", Level.CRITICAL },
        { "error", Level.ERROR },
        { "warning", Level.WARNING },
        { "info", Level.INFO },
        { "debug", Level.DEBUG },
        { "CRITICAL",  Level.CRITICAL },
        { "ERROR", Level.ERROR },
        { "WARNING", Level.WARNING },
        { "INFO", Level.INFO },
        { "DEBUG", Level.DEBUG },
        { "no_exist", null }
    });
  }

  @Test
  public void shouldGetLevelByName() {
    assertThat(Level.lookupByName(name), is(expected));
  }
}
