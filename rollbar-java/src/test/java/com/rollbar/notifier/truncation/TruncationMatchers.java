package com.rollbar.notifier.truncation;

import com.rollbar.api.json.JsonSerializable;
import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.json.JsonSerializerImpl;
import com.rollbar.notifier.sender.json.JsonTestHelper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class TruncationMatchers {
  private static final JsonSerializerImpl SERIALIZER = new JsonSerializerImpl();

  /**
   * Checks that a Payload instance matches the expected value, excluding the provided path,
   * which can differ.
   *
   * @param expected        The expected value.
   * @param pathThatDiffers The path that is allowed to be different between expected and actual.
   * @return A Matcher instance that verifies the assertion.
   */
  public static Matcher<Payload> differsOnlyBy(Payload expected, String[] pathThatDiffers) {
    final TestString expectedJson = getJsonString(expected, pathThatDiffers);
    final Matcher<Object> instanceMatcher = instanceOf(Payload.class);
    final Matcher<TestString> valueMatcher = equalTo(expectedJson);

    return new BaseMatcher<Payload>() {
      @Override
      public boolean matches(Object item) {
        if (!instanceMatcher.matches(item)) {
          return false;
        }

        Payload actual = (Payload) item;
        TestString otherJson = getJsonString(actual, pathThatDiffers);

        return valueMatcher.matches(otherJson);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Payload with JSON value " + expectedJson);
      }

      @Override
      public void describeMismatch(Object item, Description description) {
        if (!instanceMatcher.matches(item)) {
          instanceMatcher.describeMismatch(item, description);
        } else {
          Payload actual = (Payload) item;
          TestString otherJson = getJsonString(actual, pathThatDiffers);

          valueMatcher.describeMismatch(otherJson, description);
        }
      }
    };
  }

  public static Matcher<Payload> hasNoStringsLongerThan(int length) {
    final Matcher<Object> instanceMatcher = instanceOf(Payload.class);

    return new BaseMatcher<Payload>() {
      @Override
      public boolean matches(Object item) {
        if (!instanceMatcher.matches(item)) {
          return false;
        }

        String longString = findStringLongerThan(item, length);
        return longString == null;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Payload with all strings of length " + length + " or shorter.");
      }

      @Override
      public void describeMismatch(Object item, Description description) {
        if (!instanceMatcher.matches(item)) {
          instanceMatcher.describeMismatch(item, description);
        } else {
          Payload actual = (Payload) item;
          String longString = findStringLongerThan(actual, length);
          description.appendText("Found string with length " + longString.length() +
              ": " + longString);
        }
      }
    };
  }

  private static String findStringLongerThan(Object value, int length) {
    if (value == null) {
      return null;
    } else if (value instanceof Payload) {
      // By converting to a JSON map we can be sure we're checking every string, recursively.
      return findStringLongerThan(toJsonMap((Payload) value), length);
    } else if (value instanceof String) {
      if (((String) value).length() > length) {
        return (String) value;
      }
    } else if (value instanceof Iterable) {
      @SuppressWarnings("rawtypes")
      Iterable valueAsIterable = (Iterable) value;
      for (Object element : valueAsIterable) {
        String result = findStringLongerThan(element, length);
        if (result != null) {
          return result;
        }
      }
    } else if (value.getClass().isArray()) {
      int arrayLength = Array.getLength(value);
      for (int j = 0; j < arrayLength; ++j) {
        Object element = Array.get(value, j);
        String result = findStringLongerThan(element, length);
        if (result != null) {
          return result;
        }
      }
    } else if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> valueAsMap = (Map<String, Object>) value;
      for (Map.Entry<String, Object> entry : valueAsMap.entrySet()) {
        String result = findStringLongerThan(entry.getValue(), length);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  private static Map<String, Object> toJsonMap(Payload value) {
    String json = SERIALIZER.toJson(value);
    return JsonTestHelper.fromString(json);
  }

  private static TestString getJsonString(Payload payload, String[] ignoredPath) {
    Map<String, Object> otherMap = toPlainObjects(payload.asJson());
    replaceWithNull(otherMap, ignoredPath);
    return TestString.of(SERIALIZER.toJson(otherMap));
  }

  private static Map<String, Object> toPlainObjects(Map<String, Object> values) {
    Map<String, Object> result = new HashMap<>();
    for (String k : values.keySet()) {
      Object value = values.get(k);
      if (value instanceof JsonSerializable) {
        value = ((JsonSerializable) value).asJson();
      }
      result.put(k, toPlainObject(value));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static Object toPlainObject(Object jsonObject) {
    if (jsonObject instanceof Map) {
      return toPlainObjects((Map<String, Object>) jsonObject);
    } else if (jsonObject instanceof Iterable) {
      List<Object> newElements = new ArrayList<>();
      for (Object element : ((Iterable<Object>) jsonObject)) {
        newElements.add(toPlainObject(element));
      }
      return newElements.toArray(new Object[0]);
    } else {
      return jsonObject;
    }
  }

  @SuppressWarnings("unchecked")
  private static void replaceWithNull(Map<String, Object> map, String[] path) {
    Map<String, Object> current = map;
    for (int j = 0; ; ++j) {
      String key = path[j];

      if (j == path.length - 1) {
        current.put(key, null);
        return;
      }

      current = (Map<String, Object>) current.get(key);
    }
  }
}
