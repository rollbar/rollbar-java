package com.rollbar.api.payload.data;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TelemetryEventTest {
  private HashMap<String, String> defaultBody;
  private static final String TEN_CHARACTERS = "0123456789";
  private static final String BODY_KEY = "someKey";
  TelemetryEventBuilder builder;

  @Before
  public void setup() {
    builder = new TelemetryEventBuilder();
    defaultBody = makeBody();
  }

  @Test
  public void anInstanceShouldBeEqualToIt() {
    TelemetryEvent telemetryEvent = builder.build();

    assertThat(telemetryEvent, is(telemetryEvent));
  }

  @Test
  public void shouldNotBeEqualToNull() {
    TelemetryEvent telemetryEvent = builder.build();

    assertNotEquals(telemetryEvent, null);
  }

  @Test
  public void shouldNotBeEqualToADifferentObject() {
    Object anotherObject = "anotherObject";
    TelemetryEvent telemetryEvent = builder.build();

    assertNotEquals(telemetryEvent, anotherObject);
  }

  @Test
  public void shouldNotBeEqualToADifferentTypeOfTelemetryEvent() {
    TelemetryEvent manualEvent = builder.setTelemetryType(TelemetryType.MANUAL).build();
    TelemetryEvent logEvent = builder.setTelemetryType(TelemetryType.LOG).build();

    boolean equality = manualEvent.equals(logEvent);

    assertFalse(equality);
  }

  @Test
  public void shouldNotBeEqualIfTheyHaveDifferentLevel() {
    TelemetryEvent debugEvent = builder.setLevel(Level.DEBUG).build();
    TelemetryEvent criticalEvent = builder.setLevel(Level.CRITICAL).build();

    boolean equality = debugEvent.equals(criticalEvent);

    assertFalse(equality);
  }

  @Test
  public void shouldNotBeEqualIfTheyHaveDifferentTimestamp() {
    TelemetryEvent firstEvent = builder.setTimestamp(10L).build();
    TelemetryEvent secondEvent = builder.setTimestamp(11L).build();

    boolean equality = firstEvent.equals(secondEvent);

    assertFalse(equality);
  }

  @Test
  public void shouldNotBeEqualIfTheyHaveDifferentSource() {
    TelemetryEvent clientEvent = builder.setSource("client").build();
    TelemetryEvent serverEvent = builder.setSource("server").build();

    boolean equality = clientEvent.equals(serverEvent);

    assertFalse(equality);
  }

  @Test
  public void shouldNotBeEqualIfTheyHaveDifferentBody() {
    TelemetryEvent telemetryEvent1 = builder.setBody(defaultBody).build();
    defaultBody.put(BODY_KEY, TEN_CHARACTERS + TEN_CHARACTERS);
    TelemetryEvent telemetryEvent2 = builder.setBody(defaultBody).build();

    boolean equality = telemetryEvent1.equals(telemetryEvent2);

    assertFalse(equality);
  }


  @Test
  public void twoTelemetryEventsShouldBeEqualIfTheyHaveTheSameValues() {
    TelemetryEvent telemetryEvent1 = builder.setBody(defaultBody).build();
    TelemetryEvent telemetryEvent2 = builder.setBody(defaultBody).build();

    boolean equality = telemetryEvent1.equals(telemetryEvent2);

    assertTrue(equality);
  }

  @Test
  public void twoTelemetryEventsShouldHaveTheSameHashCodeIfTheyHaveTheSameValues() {
    TelemetryEvent telemetryEvent1 = builder.setBody(defaultBody).build();
    TelemetryEvent telemetryEvent2 = builder.setBody(defaultBody).build();

    int hashCode1 = telemetryEvent1.hashCode();
    int hashCode2 = telemetryEvent2.hashCode();

    assertThat(hashCode1, is(hashCode2));
  }

  @Test
  public void shouldReturnAJsonRepresentation() {
    defaultBody.put("someKey", "someValue");
    HashMap<String, Object> expected = new HashMap<>();
    expected.put("type", "log");
    expected.put("level", "debug");
    expected.put("source", TelemetryEventBuilder.DEFAULT_SOURCE);
    expected.put("timestamp_ms", TelemetryEventBuilder.DEFAULT_TIMESTAMP);
    expected.put("body", defaultBody);
    TelemetryEvent telemetryEvent1 = builder.setBody(defaultBody).build();

    Map<String, Object> json = telemetryEvent1.asJson();

    assertThat(json, is(expected));
  }


  @Test
  public void shouldReturnAStringRepresentation() {
    String expected = "TelemetryEvent{type='log', level='debug', source='" + TelemetryEventBuilder.DEFAULT_SOURCE + '\'' +
        ", timestamp_ms=" + TelemetryEventBuilder.DEFAULT_TIMESTAMP +
        ", body=" + defaultBody +
        '}';
    TelemetryEvent telemetryEvent1 = builder.setBody(defaultBody).build();

    String stringRepresentation = telemetryEvent1.toString();

    assertThat(stringRepresentation, is(expected));
  }

  @Test
  public void shouldTruncateBody() {
    HashMap<String, String> body = new HashMap<>();
    body.put("short", TEN_CHARACTERS);
    body.put("long", TEN_CHARACTERS + TEN_CHARACTERS);
    TelemetryEvent expected = builder.setBody(body).build();
    body.put("long", TEN_CHARACTERS + TEN_CHARACTERS + TEN_CHARACTERS);
    TelemetryEvent originalTelemetryEvent = builder.setBody(body).build();

    TelemetryEvent telemetryEventTruncated = originalTelemetryEvent.truncateStrings(20);

    assertThat(telemetryEventTruncated, is(expected));
  }

  private HashMap<String, String> makeBody() {
    HashMap<String, String> body = new HashMap<>();
    body.put(BODY_KEY, TEN_CHARACTERS);
    return body;
  }

}

class TelemetryEventBuilder {
  public static long DEFAULT_TIMESTAMP = 10L;
  public static String DEFAULT_SOURCE = "client";
  private long timestamp = DEFAULT_TIMESTAMP;
  private TelemetryType telemetryType = TelemetryType.LOG;
  private Level level = Level.DEBUG;
  private String source = DEFAULT_SOURCE;
  HashMap<String, String> body = new HashMap<>();

  TelemetryEventBuilder setTelemetryType(TelemetryType telemetryType) {
    this.telemetryType = telemetryType;
    return this;
  }

  TelemetryEventBuilder setLevel(Level level) {
    this.level = level;
    return this;
  }

  TelemetryEventBuilder setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  TelemetryEventBuilder setSource(String source) {
    this.source = source;
    return this;
  }

  TelemetryEventBuilder setBody(HashMap<String, String> body) {
    this.body = body;
    return this;
  }

  TelemetryEvent build() {
    return new TelemetryEvent(telemetryType, level, timestamp, source, body);
  }
}
