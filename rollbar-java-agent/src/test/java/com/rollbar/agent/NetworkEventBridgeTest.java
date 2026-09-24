package com.rollbar.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NetworkEventBridgeTest {

  @Test
  public void composeUrl_joinsHostWithRelativePath() {
    assertEquals("https://api.example.com/charge",
        NetworkEventBridge.composeUrl("https://api.example.com", "/charge"));
  }

  @Test
  public void composeUrl_joinsHostWithPortAndRelativePath() {
    assertEquals("http://localhost:8080/charge",
        NetworkEventBridge.composeUrl("http://localhost:8080", "/charge"));
  }

  @Test
  public void composeUrl_insertsSeparatorForPathWithoutLeadingSlash() {
    assertEquals("https://api.example.com/charge",
        NetworkEventBridge.composeUrl("https://api.example.com", "charge"));
  }

  @Test
  public void composeUrl_leavesAbsoluteRequestUriUntouched() {
    assertEquals("https://other.example.com/charge",
        NetworkEventBridge.composeUrl(
            "https://api.example.com", "https://other.example.com/charge"));
  }

  @Test
  public void composeUrl_keepsBaseWhenNestedUrlAppearsInQuery() {
    // '://' inside the query (OAuth redirect, URL shortener, proxy-style API) must not be read as
    // a scheme — otherwise the target host is dropped and sanitizing leaves a hostless path.
    assertEquals("https://api.example.com/api/redirect?url=https://other.example.com/foo",
        NetworkEventBridge.composeUrl(
            "https://api.example.com", "/api/redirect?url=https://other.example.com/foo"));
  }

  @Test
  public void composeUrl_keepsBaseWhenNestedUrlAppearsInPath() {
    assertEquals("https://api.example.com/proxy/https://other.example.com/foo",
        NetworkEventBridge.composeUrl(
            "https://api.example.com", "/proxy/https://other.example.com/foo"));
  }

  @Test
  public void composeUrl_keepsBaseWhenNestedUrlAppearsInFragment() {
    assertEquals("https://api.example.com/page#https://other.example.com",
        NetworkEventBridge.composeUrl(
            "https://api.example.com", "/page#https://other.example.com"));
  }

  @Test
  public void composeUrl_withoutBase_returnsRequestUri() {
    assertEquals("/charge", NetworkEventBridge.composeUrl(null, "/charge"));
  }

  @Test
  public void composeUrl_withoutRequestUri_returnsBase() {
    assertEquals("https://api.example.com",
        NetworkEventBridge.composeUrl("https://api.example.com", null));
    assertEquals("https://api.example.com",
        NetworkEventBridge.composeUrl("https://api.example.com", ""));
  }

  @Test
  public void composeUrl_withNeither_returnsEmptyString() {
    assertEquals("", NetworkEventBridge.composeUrl(null, null));
  }
}
