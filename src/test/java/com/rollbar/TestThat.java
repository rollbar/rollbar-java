package com.rollbar;

import com.rollbar.payload.utilities.Extensible;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by chris on 11/20/15.
 */
@SuppressWarnings("unchecked")
public class TestThat {
    public static <T, U> void getAndSetWorks(T t, U first, U second, GetAndSet<T, U> getAndSet) {
        assertTrue("first and second must be different for accurate test results", !first.equals(second));

        T withFirst = getAndSet.set(t, first);
        assertNotSame(t, withFirst);

        U firstGet = getAndSet.get(withFirst);
        assertEquivalent(first, firstGet);
        onlyDiffersByOneField(t, withFirst);

        T withSecond = getAndSet.set(withFirst, second);
        assertNotSame(t, withSecond);

        U secondGet = getAndSet.get(withSecond);
        assertEquivalent(second, secondGet);
        onlyDiffersByOneField(t, withSecond);
    }

    private static <U> boolean areDifferent(U before, U after) {
        if (before == null || after == null) {
            return before != after;
        }
        return getDifference(before, after) != null;
    }

    /**
     * Returns a null string if not equivalent, or the differing item if they aren't.
     * @param beforeResult
     * @param afterResult
     * @param <T>
     * @param <U>
     * @return null if equivalent, the difference if they aren't
     */
    private static <U> String getDifference(U beforeResult, U afterResult) {
        Class klass = beforeResult.getClass();

        if (Map.class.isAssignableFrom(klass)) {
            Map firstGetMap = (Map) afterResult;
            Map firstMap = (Map) beforeResult;
            if (afterResult == beforeResult) {
                return "map types should be cloned before returning";
            }
            if (firstGetMap.size() != firstMap.size()) {
                return "before and after are map types with differing sizes";
            }

            HashSet exclusion = new HashSet(firstMap.keySet());
            exclusion.addAll(firstGetMap.keySet());
            HashSet intersect = new HashSet(firstMap.keySet());
            intersect.retainAll(firstGetMap.keySet());
            exclusion.removeAll(intersect);
            if (!exclusion.isEmpty()) {
                String missed = String.join(", ", exclusion);
                return String.format("Keys in map type mismatched, differ at: %s", missed);
            }

            for(Object e: firstGetMap.keySet()) {
                if(firstGetMap.get(e) != firstMap.get(e)) {
                    return String.format("map types differ at key %s", e);
                }
            }
        } else if (Collection.class.isAssignableFrom(klass) || klass.isArray()) {
            boolean isArr = klass.isArray();
            List firstList = new ArrayList(isArr ? Arrays.asList((Object[]) beforeResult) : (Collection) beforeResult);
            List firstGetList = new ArrayList(isArr ? Arrays.asList((Object[]) afterResult) : (Collection) afterResult);

            if (firstList.size() != firstGetList.size()) {
                return "Sequence Types differ in size";
            }

            for(int i = 0; i < firstList.size(); i++) {
                Object firstEl = firstList.get(i);
                Object firstGetEl = firstGetList.get(i);
                if (firstEl != firstGetEl) {
                    return String.format("Sequence types differ at index %d", i);
                }
            }
        } else {
            if (!afterResult.equals(beforeResult)) {
                return "before and after are different, non-collection-type, objects";
            }
        }
        return null;
    }

    private static <T, U> void assertEquivalent(U beforeResult, U afterResult) {
        String difference = getDifference(beforeResult, afterResult);
        if (difference != null) {
            fail(difference);
        }
    }

    private static <T> void onlyDiffersByOneField(T first, T second) {
        Class c = first.getClass();
        assertEquals("Runtime types aren't the same!", c, second.getClass());

        Field[] fields = c.getDeclaredFields();
        Field membersField = null;
        if (Extensible.class.isAssignableFrom(c)) {
            try {
                membersField = Extensible.class.getDeclaredField("members");
            } catch (NoSuchFieldException e) {
                fail("Such a field exists");
            }
        }

        HashSet<String> differingFields = new HashSet<String>();
        for (Field f: fields) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            f.setAccessible(true);
            Object one = null;
            try {
                one = f.get(first);
                Object two = f.get(second);
                if (areDifferent(one, two)) {
                    differingFields.add(f.getName());
                }
            } catch (IllegalAccessException e) {
               fail("This can't happen since I setAccessible to true");
            }
            f.setAccessible(false);
        }

        if (membersField != null) {
            membersField.setAccessible(true);
            try {
                HashMap<String, Object> membersFirst = (HashMap<String, Object>) membersField.get(first);
                HashMap<String, Object> membersSecond = (HashMap<String, Object>) membersField.get(second);
                for(String key: membersFirst.keySet()) {
                    addIfKeysDiffer(differingFields, membersFirst, membersSecond, key);
                }
                for(String key: membersSecond.keySet()) {
                    addIfKeysDiffer(differingFields, membersFirst, membersSecond, key);
                }
            } catch (IllegalAccessException e) {
                fail("This can't happen since I setAccessible to true");
            } catch (ClassCastException e) {
                fail("members *are* return HashMap<String, Object>");
            }
            membersField.setAccessible(false);
        }

        if (differingFields.size() == 0) {
            fail("No differing fields!");
        }
        if (differingFields.size() > 1) {
            fail(String.format("More than one field differs! (%s)", String.join(", ", differingFields)));
        }
    }

    private static void addIfKeysDiffer(HashSet<String> differingFields, HashMap<String, Object> membersFirst, HashMap<String, Object> membersSecond, String key) {
        Object one = membersFirst.getOrDefault(key, null);
        Object two = membersSecond.getOrDefault(key, null);
        if (areDifferent(one, two)) {
            differingFields.add(key);
        }
    }
}
