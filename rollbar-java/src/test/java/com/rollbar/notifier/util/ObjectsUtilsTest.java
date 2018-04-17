package com.rollbar.notifier.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

public class ObjectsUtilsTest {
  private static final String[] HASH_VALUES = {"one", "two", "three"};
  private static final String TEST = "Test";
  private static final String ERROR_DESCRIPTION = "Error description";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  
  @Mock
  InputStream input;
	
  @Test
  public void testHash() {
    int result = ObjectsUtils.hash((Object[])null);
    int expected = 0;
    assertThat(result, is(expected));
    result = ObjectsUtils.hash("one", "two", "three");
    expected = Arrays.hashCode(HASH_VALUES);
    assertThat(result, is(expected));
  }

  @Test
  public void  testNonNull() {
    String result = ObjectsUtils.requireNonNull(TEST, ERROR_DESCRIPTION);
    String expected = TEST;
    assertThat(result, is(expected));
  }

  @Test
  public void  testNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(ERROR_DESCRIPTION);
    ObjectsUtils.requireNonNull(null, ERROR_DESCRIPTION);
  }
	
  @Test
  public void testStreamClosing() throws IOException {
    ObjectsUtils.close(input);
    verify(input).close();
  }
  
  @RunWith(Parameterized.class)
  public static class EqualityTest {

    @Parameterized.Parameters
    public static Object[] parametersToTestEquality() {
      return new Object[] {
        new Object[] {123, 123, true},
        new Object[] {123, 456, false},
        new Object[] {123, "123", false},
        new Object[] {"123", "123", true},
        new Object[] {"123", "test", false},
        new Object[] {null, "123", false},
        new Object[] {123, null, false},
        new Object[] {null, null, true}
      };
    }

    private Object object1;
    private Object object2;
    private boolean expected;

    public EqualityTest(Object object1, Object object2, boolean expected) {
      this.object1 = object1;
      this.object2 = object2;
      this.expected = expected;
    }

    @Test
    public void testEquals() {
      assertThat(ObjectsUtils.equals(object1, object2), is(expected));
    }

  }
}


