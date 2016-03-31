package com.rollbar.testing;

/**
 * Created by chris on 11/20/15.
 */
public interface GetAndSet<T, U> {
    U get(T t);
    T set(T t, U val);
}
