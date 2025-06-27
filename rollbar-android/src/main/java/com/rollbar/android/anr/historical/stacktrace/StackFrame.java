package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.HashMap;
import java.util.Map;

public class StackFrame implements JsonSerializable {
  private static final String MODULE_KEY = "module";
  private static final String PACKAGE_KEY = "package";
  private static final String FILENAME_KEY = "filename";
  private static final String FUNCTION_KEY = "function";
  private static final String LINE_NUMBER_KEY = "lineno";

  private String filename = "";
  private String function = "";
  private String module = "";
  private Integer lineno = 0;
  private String _package;

  public StackTraceElement toStackTraceElement() {
    return new StackTraceElement(module, function, filename, lineno);
  }

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

  @Override
  public Object asJson() {
    Map<String, Object> values = new HashMap<>();
    if (filename != null) {
      values.put(FILENAME_KEY, filename);
    }
    if (function != null) {
      values.put(FUNCTION_KEY, function);
    }
    if (module != null) {
      values.put(MODULE_KEY, module);
    }
    if (lineno != null) {
      values.put(LINE_NUMBER_KEY, lineno);
    }
    if (_package != null) {
      values.put(PACKAGE_KEY, _package);
    }
    return values;
  }
}
