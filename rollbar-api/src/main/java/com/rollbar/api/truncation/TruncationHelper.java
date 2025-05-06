package com.rollbar.api.truncation;

import com.rollbar.api.annotations.Unstable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Unstable
public class TruncationHelper {
  /**
   * Truncates all the strings in the list to the specified maximum length.
   * @param values The strings to be truncated.
   * @param maxLength Maximum length of each string.
   * @return A list containing the truncated strings.
   */
  public static List<String> truncateStringsInList(List<String> values, int maxLength) {
    if (values == null) {
      return null;
    }

    List<String> result = new ArrayList<>(values.size());
    for (String value : values) {
      result.add(truncateString(value, maxLength));
    }

    return result;
  }

  /**
   * Truncates all the StringTruncatable in the list to the specified maximum length.
   * @param values The StringTruncatables to be truncated.
   * @param maxLength Maximum length of each string.
   * @return A list containing the truncated StringTruncatables.
   */
  public static <T extends StringTruncatable<T>> List<T> truncate(List<T> values, int maxLength) {
    if (values == null) {
      return null;
    }

    List<T> result = new ArrayList<>(values.size());
    for (T value : values) {
      result.add(value.truncateStrings(maxLength));
    }

    return result;
  }

  /**
   * Truncates any strings in the list to the specified maximum length.
   * @param values The list of objects which might contain strings to be truncated.
   * @param maxLength Maximum length of each string.
   * @return A list with a copy of the values, where any string value has been truncated.
   */
  public static List<Object> truncateStringsInObjectList(List<Object> values, int maxLength) {
    if (values == null) {
      return null;
    }

    List<Object> result = new ArrayList<>(values.size());
    for (Object value : values) {
      if (value instanceof String) {
        result.add(truncateString((String) value, maxLength));
      } else {
        result.add(value);
      }

    }

    return result;
  }

  /**
   * Truncates the strings in the object implementing StringTruncatable.
   * @param value The value whose strings must be truncated.
   * @param maxLength The maximum length of the strings.
   * @param <T> The type of the value.
   * @return A T instance with its strings truncated.
   */
  public static <T extends StringTruncatable<T>> T truncateStringsInObject(T value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.truncateStrings(maxLength);
  }

  /**
   * Truncates the string to the specified maximum length.
   * @param original The string to be truncated.
   * @param maxLength The maximum length.
   * @return The string truncated to the specified maximum length.
   */
  public static String truncateString(String original, int maxLength) {
    if (original == null || original.length() <= maxLength) {
      return original;
    }

    return original.substring(0, maxLength);
  }

  /**
   * Truncates strings found in a map of lists of strings.
   * @param values The map of lists of strings.
   * @param maxLength The maximum length of each string.
   * @return A map with all the strings truncated to the specified maximum length.
   */
  public static Map<String, List<String>> truncateStringsInStringListMap(
      Map<String, List<String>> values, int maxLength) {
    if (values == null || values.isEmpty()) {
      return values;
    }

    Map<String, List<String>> result = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : values.entrySet()) {
      result.put(entry.getKey(), truncateStringsInList(entry.getValue(), maxLength));
    }

    return result;
  }

  /**
   * Truncates strings found in a map of strings.
   * @param values The map of strings.
   * @param maxLength The maximum length of each string.
   * @return A map with all the strings truncated to the specified maximum length.
   */
  public static Map<String, String> truncateStringsInStringMap(Map<String, String> values,
                                                               int maxLength) {
    if (values == null || values.isEmpty()) {
      return values;
    }

    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      result.put(entry.getKey(), truncateString(entry.getValue(), maxLength));
    }

    return result;
  }

  /**
   * Truncates strings found in a map of maps of objects.
   * @param values The map of maps.
   * @param maxLength The maximum length of each string.
   * @return A map with all the strings truncated to the specified maximum length.
   */
  public static Map<String, Map<String, Object>> truncateStringsInNestedMap(
      Map<String, Map<String, Object>> values, int maxLength) {
    Map<String, Map<String, Object>> result = new HashMap<>();

    for (Map.Entry<String, Map<String, Object>> entry : values.entrySet()) {
      result.put(entry.getKey(), truncateStringsInMap(entry.getValue(), maxLength));
    }

    return result;
  }

  /**
   * Truncates strings found in a map of objects.
   * @param values The map of objects.
   * @param maxLength The maximum length of each string.
   * @return A map with all the strings truncated to the specified maximum length.
   */
  public static Map<String, Object> truncateStringsInMap(Map<String, Object> values,
                                                         int maxLength) {
    if (values == null || values.isEmpty()) {
      return values;
    }

    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String) {
        value = truncateString((String)value, maxLength);
      }
      result.put(entry.getKey(), value);
    }
    return result;
  }
}
