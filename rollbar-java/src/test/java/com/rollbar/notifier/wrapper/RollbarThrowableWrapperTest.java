package com.rollbar.notifier.wrapper;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import org.junit.Test;

public class RollbarThrowableWrapperTest {

  @Test
  public void shouldCollectThreads() {
    RollbarThrowableWrapper sut = new RollbarThrowableWrapper(new Exception("Any"), Thread.currentThread());

    assertNotNull(sut.getAllStackTraces());
    assertNotNull(sut.getRollbarThread());
  }

  @Test
  public void shouldBeCreatedByThrowable() {
    Throwable nestedThrowable = new IllegalStateException("This is the nested throwable message");
    Throwable throwable = new IllegalArgumentException("This is the root throwable message", nestedThrowable);

    ThrowableWrapper nested = new RollbarThrowableWrapper(nestedThrowable);
    RollbarThrowableWrapper sut = new RollbarThrowableWrapper(throwable);

    assertThat(sut.getClassName(), is(throwable.getClass().getName()));
    assertThat(sut.getMessage(), is(throwable.getMessage()));
    assertThat(sut.getStackTrace(), is(throwable.getStackTrace()));
    assertThat(sut.getCause(), is(nested));
    assertThat(sut.getThrowable(), is(throwable));
    assertNotNull(sut.getAllStackTraces());
    assertNotNull(sut.getRollbarThread());
  }

  @Test
  public void shouldBeCreatedByThrowableParams() {
    String className = NullPointerException.class.getName();
    String message = "This is the throwable message";
    StackTraceElement[] elements = new StackTraceElement[1];
    elements[0] = new StackTraceElement("className", "logMethod", "fileName",3);

    ThrowableWrapper cause = new RollbarThrowableWrapper(new IllegalAccessError());

    RollbarThrowableWrapper sut = new RollbarThrowableWrapper(className,message, elements, cause);

    assertThat(sut.getClassName(), is(className));
    assertThat(sut.getMessage(), is(message));
    assertThat(sut.getStackTrace(), is(elements));
    assertThat(sut.getCause(), is(cause));
    assertThat(sut.getThrowable(), is(nullValue()));
    assertNull(sut.getAllStackTraces());
    assertNull(sut.getRollbarThread());
  }

  @Test
  public void shouldBeEqual() {
    Throwable throwable = new IllegalArgumentException("This is the throwable message");

    RollbarThrowableWrapper sut1 = new RollbarThrowableWrapper(throwable);
    RollbarThrowableWrapper sut2 = new RollbarThrowableWrapper(throwable);

    assertEquals(sut1, sut2);
  }
}