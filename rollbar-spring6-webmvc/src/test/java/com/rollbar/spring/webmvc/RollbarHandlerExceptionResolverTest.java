package com.rollbar.spring.webmvc;

import com.rollbar.notifier.Rollbar;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class RollbarHandlerExceptionResolverTest {

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

        // construct exception resolver from the Rollbar resolver for Spring webmvc
        HandlerExceptionResolver handlerExceptionResolver = new RollbarHandlerExceptionResolver(rollbar);

        // build a full mocked out request for the exception resolver
        handlerExceptionResolver.resolveException(request, response, null, testException);

        // verify that the rollbar mocked object got the exception inside the resolver
        verify(rollbar, times(1)).error(testException);
    }

}
