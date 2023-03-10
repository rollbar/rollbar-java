package com.rollbar.web.provider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.data.Person;
import com.rollbar.web.listener.RollbarRequestListener;
import java.security.Principal;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class PersonProviderTest {

  static final String PRINCIPAL_NAME = "user";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  ServletRequestEvent requestEvent;

  @Mock
  HttpServletRequest request;

  RollbarRequestListener listener;

  PersonProvider sut;

  @Before
  public void setUp() {
    when(requestEvent.getServletRequest()).thenReturn(request);

    listener = new RollbarRequestListener();
    listener.requestInitialized(requestEvent);

    sut = new PersonProvider();
  }

  @Test
  public void shouldRetrieveThePersonIfPrincipalPresent() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(PRINCIPAL_NAME);

    when(request.getUserPrincipal()).thenReturn(principal);

    Person result = sut.provide();
    Person expected = new Person.Builder().id(PRINCIPAL_NAME).build();

    assertThat(result, is(expected));
  }

  @Test
  public void shouldRetrieveNullIfPrincipalNotPresent() {
    when(request.getUserPrincipal()).thenReturn(null);

    Person result = sut.provide();

    assertNull(result);
  }
}
