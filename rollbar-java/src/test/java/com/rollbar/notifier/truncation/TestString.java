package com.rollbar.notifier.truncation;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

/**
 * IntelliJ locks up on the assert output if the string is too large, so we wrap the strings and
 * make them something that can be displayed.
 */
class TestString {
  private static final MessageDigest MD5;

  private final String value;

  static {
    try {
      MD5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private TestString(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestString that = (TestString) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    Charset UTF8 = Charset.forName("UTF-8");
    if (value == null) {
      return "";
    } else {
      byte[] hashBytes = MD5.digest(value.getBytes(UTF8));
      String hashString = Base64.getEncoder().encodeToString(hashBytes);
      return hashString + ":" + value.substring(0, Math.min(100, value.length()));
    }
  }

  public static TestString of(String value) {
    return new TestString(value);
  }
}
