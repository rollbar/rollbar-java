package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LockReason implements JsonSerializable {

  public static final int LOCKED = 1;
  public static final int WAITING = 2;
  public static final int SLEEPING = 4;
  public static final int BLOCKED = 8;

  private int type;
  private  String address;
  private  String packageName;
  private  String className;
  private  Long threadId;
  private  Map<String, Object> unknown;

  public LockReason() {}

  public LockReason(final  LockReason other) {
    this.type = other.type;
    this.address = other.address;
    this.packageName = other.packageName;
    this.className = other.className;
    this.threadId = other.threadId;
    if (other.unknown != null) {
      this.unknown = new ConcurrentHashMap<>(other.unknown);
    }
  }

  @SuppressWarnings("unused")
  public int getType() {
    return type;
  }

  public void setType(final int type) {
    this.type = type;
  }

  
  public String getAddress() {
    return address;
  }

  public void setAddress(final  String address) {
    this.address = address;
  }

  public void setPackageName(final  String packageName) {
    this.packageName = packageName;
  }

  public void setClassName(final  String className) {
    this.className = className;
  }

  public void setThreadId(final  Long threadId) {
    this.threadId = threadId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LockReason that = (LockReason) o;
    return Objects.equals(address, that.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address);
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();
    values.put(JsonKeys.TYPE, type);
      if (address != null) {
        values.put(JsonKeys.ADDRESS, address);
      }
      if (packageName != null) {
        values.put(JsonKeys.PACKAGE_NAME, packageName);
      }
      if (className != null) {
        values.put(JsonKeys.CLASS_NAME, className);
      }
      if (threadId != null) {
        values.put(JsonKeys.THREAD_ID, threadId);
      }
      if (unknown != null) {
        for (String key : unknown.keySet()) {
          Object value = unknown.get(key);
          values.put(key, value);
        }
      }
    return values;
  }

  public static final class JsonKeys {
    public static final String TYPE = "type";
    public static final String ADDRESS = "address";
    public static final String PACKAGE_NAME = "package_name";
    public static final String CLASS_NAME = "class_name";
    public static final String THREAD_ID = "thread_id";
  }
}
