package com.rollbar.struts;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class RollbarFactoryTest {

  @Test
  public void testBuild() {
    RollbarFactory sut = new RollbarFactory();

    assertNotNull(sut.build());
  }

}
