package com.rollbar.notifier.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ObjectsUtilsTest {
  private static final Object[] EQUALS_VALUES = {"123", 123, null};
  private static final String[] HASH_VALUES = {"one", "two", "three"};
  private static String TEST = "Test";
  private static String ERROR_DESCRIPTION = "Error description";
	
  @Rule
  public ExpectedException expectedException;
	
  @Before
  public void setUp() {
    expectedException = ExpectedException.none();
  }
	
  @Test
  public void testEquals() {
    for(int i = 0; i < EQUALS_VALUES.length; i++)
	  for(int j = 0; j < EQUALS_VALUES.length; j++) { Object first = EQUALS_VALUES[i];
	    Object second = EQUALS_VALUES[j];
		  boolean expected = (i == j);
		  boolean result = ObjectsUtils.equals(first, second);
		  assertThat(String.format("Expected %b when equating %s to %s, got %b instead.", expected, first, second, result), result, is(expected));
    }
  }
		
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
}
