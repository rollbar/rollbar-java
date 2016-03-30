package com.rollbar.utilities;

import java.util.Arrays;

/**
 * Some helper methods absent in Java 7
 */
public class StringUtils {
    public static String join(CharSequence delimiter,
                              Iterable<? extends CharSequence> elements) {
        int length = 0;
        for (CharSequence elem : elements) {
            if(elem.length() == 0)
                continue;
            length += elem.length() + delimiter.length();
        }
        StringBuilder stb = new StringBuilder(length);
        for (CharSequence elem : elements) {
            if(elem.length() == 0)
                continue;
            stb.append(elem).append(delimiter);
        }
        if (stb.length() > 0)
            stb.setLength(stb.length() - delimiter.length());
        return stb.toString();
    }

    public static String join(String delimiter,
                              String[] elements) {
        return join(delimiter, Arrays.asList(elements));
    }
}
