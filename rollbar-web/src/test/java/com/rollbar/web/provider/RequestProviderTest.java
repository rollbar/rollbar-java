package com.rollbar.web.provider;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.data.Request;
import com.rollbar.web.listener.RollbarRequestListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RequestProviderTest {

  static final String QUERYSTRING = "param1=value1&param1=value2&param2=value3";

  static final StringBuffer REQUEST_URL = new StringBuffer("https://rollbar.com");

  static final String METHOD = "GET";

  static final Map<String, String> HEADERS = new HashMap<>();
  static {
    HEADERS.put("accept", "text/html,application/xhtml+xml,"
        + "application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
  }

  static final Map<String, String[]> REQUEST_GET_PARAMS = new HashMap<>();
  static {
    REQUEST_GET_PARAMS.put("param1", new String[]{"value1", "value2"});
    REQUEST_GET_PARAMS.put("param2", new String[]{"value3"});
  }

  static final Map<String, List<String>> EXPECTED_GET_PARAMS = new HashMap<>();
  static {
    for(String paramName : REQUEST_GET_PARAMS.keySet()) {
      EXPECTED_GET_PARAMS.put(paramName, asList(REQUEST_GET_PARAMS.get(paramName)));
    }
  }

  static final String REMOTE_ADDRESS = "127.0.0.1";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  ServletRequestEvent requestEvent;

  @Mock
  HttpServletRequest request;

  RollbarRequestListener listener;

  RequestProvider sut;

  @Before
  public void setUp() {
    when(request.getRequestURL()).thenReturn(REQUEST_URL);
    when(request.getMethod()).thenReturn(METHOD);
    when(request.getHeaderNames()).thenReturn(enumeration(HEADERS.keySet()));
    for(String headerName : HEADERS.keySet()) {
      when(request.getHeader(headerName)).thenReturn(HEADERS.get(headerName));
    }
    when(request.getParameterMap()).thenReturn(REQUEST_GET_PARAMS);
    when(request.getQueryString()).thenReturn(QUERYSTRING);
    when(request.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);

    when(requestEvent.getServletRequest()).thenReturn(request);

    listener = new RollbarRequestListener();
    listener.requestInitialized(requestEvent);

    sut = new RequestProvider.Builder().build();
  }

  @Test
  public void shouldRetrieveTheRequest() {
    Request result = sut.provide();

    assertThat(result.getUrl(), is(REQUEST_URL.toString()));
    assertThat(result.getMethod(), is(METHOD));
    assertThat(result.getHeaders(), is(HEADERS));
    assertThat(result.getGet(), is(EXPECTED_GET_PARAMS));
    assertThat(result.getQueryString(), is(QUERYSTRING));
    assertThat(result.getUserIp(), is(REMOTE_ADDRESS));
  }

  @Test
  public void shouldRetrieveTheRemoteAddressUsingHeader() {
    String userIpHeaderName = "X-FORWARDED-FOR";

    RequestProvider sut = new RequestProvider.Builder()
        .userIpHeaderName(userIpHeaderName)
        .build();

    String remoteAddr = "192.168.1.1";
    when(request.getHeader(userIpHeaderName)).thenReturn(remoteAddr);

    Request result = sut.provide();

    assertThat(result.getUserIp(), is(remoteAddr));
  }

  @Test
  public void shouldRetrieveTheRemoteAddressUsingRequestRemoteAddress() {
    String userIpHeaderName = "";

    RequestProvider sut = new RequestProvider.Builder()
        .userIpHeaderName(userIpHeaderName)
        .build();

    String remoteAddr = "192.168.1.1";
    when(request.getRemoteAddr()).thenReturn(remoteAddr);

    Request result = sut.provide();

    assertThat(result.getUserIp(), is(remoteAddr));
  }

  @Test
  public void shouldRetrieveTheRemoteAddressUsingRequestRemoteAddressAndAnonymize() {
    String userIpHeaderName = "";

    RequestProvider sut = new RequestProvider.Builder()
        .userIpHeaderName(userIpHeaderName)
        .captureIp("anonymize")
        .build();

    String remoteAddr = "192.168.1.1";
    String remoteAddrAnon = "192.168.1.0/24";
    when(request.getRemoteAddr()).thenReturn(remoteAddr);

    Request result = sut.provide();

    assertThat(result.getUserIp(), is(remoteAddrAnon));
  }

  @Test
  public void shouldRetrieveTheRemoteAddressUsingRequestRemoteAddressAndNotCaptureIfCaptureIpIsNone() {
    String userIpHeaderName = "";

    RequestProvider sut = new RequestProvider.Builder()
        .userIpHeaderName(userIpHeaderName)
        .captureIp("none")
        .build();

    String remoteAddr = "192.168.1.1";
    when(request.getRemoteAddr()).thenReturn(remoteAddr);

    Request result = sut.provide();

    assertNull(result.getUserIp());
  }
}
