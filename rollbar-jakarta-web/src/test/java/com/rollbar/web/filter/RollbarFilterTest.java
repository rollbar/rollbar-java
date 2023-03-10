package com.rollbar.web.filter;

import static com.rollbar.web.filter.RollbarFilter.ACCESS_TOKEN_PARAM_NAME;
import static com.rollbar.web.filter.RollbarFilter.CONFIG_PROVIDER_CLASS_PARAM_NAME;
import static com.rollbar.web.filter.RollbarFilter.USER_IP_HEADER_PARAM_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.web.config.FakeConfigProvider;
import com.rollbar.notifier.Rollbar;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RollbarFilterTest {

  static final Throwable ERROR = new RuntimeException("Something went wrong");

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  Rollbar rollbar;

  @Mock
  ServletRequest request;

  @Mock
  ServletResponse response;

  @Mock
  FilterChain chain;

  @Mock
  FilterConfig filterConfig;

  RollbarFilter sut;

  @Before
  public void setUp() throws Exception {
    doThrow(ERROR).when(chain).doFilter(request, response);

    sut = new RollbarFilter(rollbar);
  }

  @Test
  public void shouldInit() throws Exception {
    sut.init(filterConfig);

    verify(filterConfig).getInitParameter(ACCESS_TOKEN_PARAM_NAME);
    verify(filterConfig).getInitParameter(USER_IP_HEADER_PARAM_NAME);
    verify(filterConfig).getInitParameter(CONFIG_PROVIDER_CLASS_PARAM_NAME);
  }

  @Test
  public void shouldUseConfigProviderIfAvailable() throws Exception {
    when(filterConfig.getInitParameter(CONFIG_PROVIDER_CLASS_PARAM_NAME)).thenReturn(
        FakeConfigProvider.class.getCanonicalName());
    sut.init(filterConfig);

    assertTrue(FakeConfigProvider.CALLED);
  }

  @Test
  public void shouldNotUseConfigProviderIfError() throws Exception {
    when(filterConfig.getInitParameter(CONFIG_PROVIDER_CLASS_PARAM_NAME)).thenReturn(
        "com.rollbar.not.exists");
    sut.init(filterConfig);

    assertFalse(FakeConfigProvider.CALLED);
  }

  @Test
  public void shouldLogError() throws Exception {
    try {
      sut.doFilter(request, response, chain);
    } catch (Exception e) {
      if(!e.equals(ERROR)) {
        fail();
      }
    }
    verify(rollbar).error(ERROR);
  }

  @Test
  public void shouldSwallowException() throws Exception {
    doThrow(new RuntimeException("Error sending to Rollbar")).when(rollbar).
        error(any(Throwable.class));

    try {
      sut.doFilter(request, response, chain);
    } catch (Exception e) {
      if(!e.equals(ERROR)) {
        fail();
      }
    }
  }
}
