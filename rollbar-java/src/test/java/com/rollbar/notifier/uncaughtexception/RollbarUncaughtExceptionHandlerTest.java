package com.rollbar.notifier.uncaughtexception;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rollbar.notifier.Rollbar;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RollbarUncaughtExceptionHandlerTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  Rollbar rollbar;

  @Mock
  Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

  @Mock
  Thread thread;

  @Mock
  Throwable throwable;

  @Test
  public void shouldLogThrowableToRollbar() {
    RollbarUncaughtExceptionHandler sut = new RollbarUncaughtExceptionHandler(rollbar,
        null);

    sut.uncaughtException(thread, throwable);

    verify(rollbar).log(throwable, thread, null, null, null, true);
    verify(uncaughtExceptionHandler, never()).uncaughtException(thread, throwable);
  }

  @Test
  public void shouldLogThrowableToRollbarAndDelegate() {
    RollbarUncaughtExceptionHandler sut = new RollbarUncaughtExceptionHandler(rollbar,
        uncaughtExceptionHandler);

    sut.uncaughtException(thread, throwable);

    verify(rollbar).log(throwable, thread, null, null, null, true);
    verify(uncaughtExceptionHandler).uncaughtException(thread, throwable);
  }

}
