package com.rollbar.notifier.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

/**
 * Helper class that provides Java 7 features.
 */
public class ObjectsUtils {
	
	public static boolean equals(Object object1, Object object2) {
		return object1 == object2 || object1 != null && object1.equals(object2);
	}
	
	public static int hash(Object... objects) {
		return Arrays.hashCode(objects);
	}
	
	public static <T> T requireNonNull(T object, String errorMessage) {
		if (object == null) {
			throw new NullPointerException(errorMessage);
		} else {
			return object;
		}
	}
	
	public static void close(final Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
