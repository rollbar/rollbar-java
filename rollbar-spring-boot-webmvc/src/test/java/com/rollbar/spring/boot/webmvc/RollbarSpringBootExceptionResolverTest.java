package com.rollbar.spring.boot.webmvc;

import com.rollbar.notifier.Rollbar;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class RollbarSpringBootExceptionResolverTest {

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Test
    public void testRollbarExceptionResolver() {
        Exception testException = new Exception("test exception");

        // build the Rollbar mock object
        Rollbar rollbar = mock(Rollbar.class);
        doNothing().when(rollbar).error(testException);

        // construct exception resolver from the Rollbar resolver for Spring boot
        HandlerExceptionResolver handlerExceptionResolver = new RollbarSpringBootExceptionResolver(rollbar);

        // builed a full mocked out request for the exception resolver
        handlerExceptionResolver.resolveException(request, response, null, testException);

        // verify that the rollbar mocked object got the exception inside the resolver
        verify(rollbar, times(1)).error(testException);
    }

}