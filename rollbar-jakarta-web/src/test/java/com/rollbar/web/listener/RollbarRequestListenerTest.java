package com.rollbar.web.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RollbarRequestListenerTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  ServletRequestEvent requestEvent;

  @Mock
  HttpServletRequest request;

  RollbarRequestListener sut;

  @Before
  public void setUp() {
    when(requestEvent.getServletRequest()).thenReturn(request);

    sut = new RollbarRequestListener();
  }

  @Test
  public void shouldSetTheRequest() {
    sut.requestInitialized(requestEvent);

    assertThat(RollbarRequestListener.getServletRequest(), is(request));
  }

  @Test
  public void shouldRemoveTheRequest() {
    sut.requestInitialized(requestEvent);
    sut.requestDestroyed(requestEvent);

    assertNull(RollbarRequestListener.getServletRequest());
  }
}
