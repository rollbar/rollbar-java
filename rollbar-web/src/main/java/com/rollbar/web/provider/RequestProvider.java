package com.rollbar.web.provider;

import com.rollbar.api.payload.data.Request;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.web.listener.RollbarRequestListener;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link Request} provider.
 */
public class RequestProvider implements Provider<Request> {

  /**
   * Constructor.
   */
  public RequestProvider() {
  }

  @Override
  public Request provide() {
    HttpServletRequest req = RollbarRequestListener.getServletRequest();

    if(req != null) {
      Request request = new Request.Builder()
          .url(url(req))
          .method(method(req))
          .headers(headers(req))
          .get(getParams(req))
          .querystring(queryString(req))
          .userIp(userIp(req))
          .build();

      return request;
    }

    return null;
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

  private static Map<String, String> getParams(HttpServletRequest request) {
    if("GET".equalsIgnoreCase(request.getMethod())) {
      Map<String, String> params = new HashMap<>();

      Map<String, String[]> paramNames = request.getParameterMap();
      for (String paramName : paramNames.keySet()) {
        String[] values = paramNames.get(paramName);

        if (values != null && values.length > 0) {
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < values.length; i++) {
            sb.append(values[i]);
            if (i + 1 != values.length) {
              sb.append(',');
            }
          }
          params.put(paramName, sb.toString());
        }
      }

      return params;
    }

    return null;
  }

  private static String queryString(HttpServletRequest request) {
    return request.getQueryString();
  }

  private static String userIp(HttpServletRequest request) {
    String remoteAddr = request.getHeader("X-FORWARDED-FOR");

    if (remoteAddr == null || "".equals(remoteAddr)) {
      remoteAddr = request.getRemoteAddr();
    }

    return remoteAddr;
  }

}
