package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.HashMap;
import java.util.Map;

public class StackFrame implements JsonSerializable {

  private String filename = "";
  private String function = "";
  private String module = "";
  private Integer lineno = 0;
  private String _package;

  public StackTraceElement toStackTraceElement() {
    return new StackTraceElement(module, function, filename, lineno);
  }

  private LockReason lock;

  public void setFilename(final  String filename) {
    this.filename = filename;
  }

  public void setFunction(final  String function) {
    this.function = function;
  }

  public void setModule(final  String module) {
    this.module = module;
  }

  public void setLineno(final  Integer lineno) {
    this.lineno = lineno;
  }

  public  String getPackage() {
    return _package;
  }

  public void setPackage(final  String _package) {
    this._package = _package;
  }

  public void setLock(final  LockReason lock) {
    this.lock = lock;
  }

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();
    if (filename != null) {
      values.put(JsonKeys.FILENAME, filename);
    }
    if (function != null) {
      values.put(JsonKeys.FUNCTION, function);
    }
    if (module != null) {
      values.put(JsonKeys.MODULE, module);
    }
    if (lineno != null) {
      values.put(JsonKeys.LINENO, lineno);
    }
    if (_package != null) {
      values.put(JsonKeys.PACKAGE, _package);
    }
    if (lock != null) {
      values.put(JsonKeys.LOCK, lock);
    }
    return values;
  }

  public static final class JsonKeys {
    public static final String FILENAME = "filename";
    public static final String FUNCTION = "function";
    public static final String MODULE = "module";
    public static final String LINENO = "lineno";
    public static final String PACKAGE = "package";
    public static final String LOCK = "lock";
  }
}
