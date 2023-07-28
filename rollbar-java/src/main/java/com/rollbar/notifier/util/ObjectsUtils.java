package com.rollbar.notifier.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that provides Java 7 features.
 */
public class ObjectsUtils {

  private static class LogInstance {
    private static final Logger INSTANCE = LoggerFactory.getLogger(ObjectsUtils.class);
  }

  private static Logger logger() {
    return LogInstance.INSTANCE;
  }


  public static boolean equals(Object object1, Object object2) {
    return object1 == object2 || object1 != null && object1.equals(object2);
  }

  public static int hash(Object... objects) {
    return Arrays.hashCode(objects);
  }

  /**
   * Checks that the specified object reference is not null.
   *
   * @param object the object reference to check for nullity
   * @param errorMessage detail message to be used in the event that a NullPointerException is
   *                     thrown
   * @param <T> the type of the reference
   * @return object if not null
   */
  public static <T> T requireNonNull(T object, String errorMessage) {
    if (object == null) {
      throw new NullPointerException(errorMessage);
    } else {
      return object;
    }
  }

  /**
   * Closes stream if possible.
   *
   * @param closeable the closable implementation to close
   */
  public static void close(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException e) {
      logger().error("Unable to close stream.", e);
    }
  }
}