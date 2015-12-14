package com.rollbar.payload.utilities;

/**
 * An object that can be serialized to JSON
 */
public interface JsonSerializable {
    Object asJson();
}
