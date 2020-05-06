package com.rollbar.springboot.web;

import org.junit.Test;
import com.rollbar.notifier.Rollbar;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class RollbarExceptionResolverTest {

    @Test
    public void testRollbarExceptionResolver() {
        Exception testException = new Exception("test exception");

        // build the Rollbar mock object
        Rollbar rollbar = mock(Rollbar.class);
        doNothing().when(rollbar).error(testException);

        // construct exception resolver from the Rollbar resolver for Spring boot
        HandlerExceptionResolver handlerExceptionResolver = new RollbarExceptionResolver(rollbar);

        // builed a full mocked out request for the exception resolver
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        handlerExceptionResolver.resolveException(request, response, null, testException);

        // verify that the rollbar mocked object got the exception inside the resolver
        verify(rollbar, times(1)).error(testException);
    }

}