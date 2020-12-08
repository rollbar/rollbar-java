package com.rollbar.struts.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opensymphony.xwork2.ActionInvocation;
import com.rollbar.notifier.Rollbar;
import com.rollbar.struts.RollbarFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RollbarExceptionInterceptorTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private RollbarExceptionInterceptor sut;

  @Mock
  private RollbarFactory rollbarFactory;

  @Mock
  private Rollbar rollbar;

  @Mock
  private ActionInvocation action;

  @Before
  public void setUp() {
    when(rollbarFactory.build()).thenReturn(rollbar);

    sut = new RollbarExceptionInterceptor(rollbarFactory);
  }

  @Test
  public void testInitWithDI() throws Exception {
    sut.init();

    assertEquals(rollbar, sut.getRollbar());
  }

  @Test
  public void testInitWithoutDI() throws Exception {
    String accessToken = "test_access_token";

    sut = new RollbarExceptionInterceptor();
    sut.setAccessToken(accessToken);

    sut.init();

    assertNotNull(sut.getRollbar());
  }

  @Test
  public void testIntercept() throws Exception {
    Exception exception = mock(Exception.class);

    when(action.invoke()).thenThrow(exception);

    sut.init();

    try {
      sut.intercept(action);
    } catch (Exception e) {
      if(!e.equals(exception)) {
        fail();
      }
    }
    verify(rollbar).error(exception);
  }
}
