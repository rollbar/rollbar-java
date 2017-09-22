package com.rollbar.api.payload.data.body;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.rollbar.test.Factory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class CodeContextTest {

  @Test
  public void shouldBeEqual() {
    CodeContext codeContext1 = Factory.codeContext();
    CodeContext codeContext2 = Factory.codeContext();

    assertThat(codeContext2, is(codeContext1));
  }

  @Test
  public void shouldReturnAsJson() {
    CodeContext codeContext = Factory.codeContext();

    Map<String, Object> expected = new HashMap<>();

    if(codeContext.getPre() != null) expected.put("pre", codeContext.getPre());
    if(codeContext.getPost() != null) expected.put("post", codeContext.getPost());

    assertThat(codeContext.asJson(), is(expected));
  }
}