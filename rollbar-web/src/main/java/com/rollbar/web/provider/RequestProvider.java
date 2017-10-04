package com.rollbar.web.provider;

import static java.util.Arrays.asList;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.Request;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.web.listener.RollbarRequestListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link Request} provider.
 */
public class RequestProvider implements Provider<Request> {

  private final String userIpHeaderName;

  /**
   * Constructor.
   */
  RequestProvider(Builder builder) {
    this.userIpHeaderName = builder.userIpHeaderName;
  }

  @Override
  public Request provide() {
    HttpServletRequest req = RollbarRequestListener.getServletRequest();

    if (req != null) {
      Request request = new Request.Builder()
          .url(url(req))
          .method(method(req))
          .headers(headers(req))
          .get(getParams(req))
          .queryString(queryString(req))
          .userIp(userIp(req))
          .build();

      return request;
    }

    return null;
  }

  private String userIp(HttpServletRequest request) {
    if (userIpHeaderName == null || "".equals(userIpHeaderName)) {
      return request.getRemoteAddr();
    }

    return request.getHeader(userIpHeaderName);
  }

  private static String url(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }

  private static String method(HttpServletRequest request) {
    return request.getMethod();
  }

  private static Map<String, String> headers(HttpServletRequest request) {
    Map<String, String> headers = new HashMap<>();

    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      headers.put(headerName, request.getHeader(headerName));
    }

    return headers;
  }

  private static Map<String, List<String>> getParams(HttpServletRequest request) {
    if ("GET".equalsIgnoreCase(request.getMethod())) {
      Map<String, List<String>> params = new HashMap<>();

      Map<String, String[]> paramNames = request.getParameterMap();
      for (Entry<String, String[]> param : paramNames.entrySet()) {
        if (param.getValue() != null && param.getValue().length > 0) {
          params.put(param.getKey(), asList(param.getValue()));
        }
      }

      return params;
    }

    return null;
  }

  private static String queryString(HttpServletRequest request) {
    return request.getQueryString();
  }

  /**
   * Builder class for {@link RequestProvider}.
   */
  public static final class Builder {

    private String userIpHeaderName;

    /**
     * The request header name to retrieve the user ip.
     * @param userIpHeaderName the header name.
     * @return the builder instance.
     */
    public Builder userIpHeaderName(String userIpHeaderName) {
      this.userIpHeaderName = userIpHeaderName;
      return this;
    }

    /**
     * Builds the {@link RequestProvider request provider}.
     *
     * @return the payload.
     */
    public RequestProvider build() {
      return new RequestProvider(this);
    }
  }
}
