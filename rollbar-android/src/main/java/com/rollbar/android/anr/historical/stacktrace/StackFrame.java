package com.rollbar.android.anr.historical.stacktrace;

import com.rollbar.api.json.JsonSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackFrame implements JsonSerializable {

  private List<String> preContext;

  private List<String> postContext;

  private Map<String, String> vars;

  private List<Integer> framesOmitted;

  private String filename = "";

  private String function = "";

  private String module = "";

  private Integer lineno = 0;

  private Integer colno;

  private String absPath;

  private String contextLine;

  private Boolean inApp;

  private String _package;

  private Boolean _native;

  private String platform;

  private String imageAddr;

  private String symbolAddr;

  private String instructionAddr;

  private String symbol;

  @SuppressWarnings("unused")
  private Map<String, Object> unknown;

  public StackTraceElement toStackTraceElement() {
    return new StackTraceElement(module, function, filename, lineno);
  }

  private String rawFunction;

  private LockReason lock;

  public List<String> getPreContext() {
    return preContext;
  }

  public void setPreContext(final List<String> preContext) {
    this.preContext = preContext;
  }

  public List<String> getPostContext() {
    return postContext;
  }

  public void setPostContext(final List<String> postContext) {
    this.postContext = postContext;
  }

  public Map<String, String> getVars() {
    return vars;
  }

  public void setVars(final Map<String, String> vars) {
    this.vars = vars;
  }

  public List<Integer> getFramesOmitted() {
    return framesOmitted;
  }

  public void setFramesOmitted(final  List<Integer> framesOmitted) {
    this.framesOmitted = framesOmitted;
  }

  public  String getFilename() {
    return filename;
  }

  public void setFilename(final  String filename) {
    this.filename = filename;
  }

  public  String getFunction() {
    return function;
  }

  public void setFunction(final  String function) {
    this.function = function;
  }

  public  String getModule() {
    return module;
  }

  public void setModule(final  String module) {
    this.module = module;
  }

  public  Integer getLineno() {
    return lineno;
  }

  public void setLineno(final  Integer lineno) {
    this.lineno = lineno;
  }

  public  Integer getColno() {
    return colno;
  }

  public void setColno(final  Integer colno) {
    this.colno = colno;
  }

  public  String getAbsPath() {
    return absPath;
  }

  public void setAbsPath(final  String absPath) {
    this.absPath = absPath;
  }

  public  String getContextLine() {
    return contextLine;
  }

  public void setContextLine(final  String contextLine) {
    this.contextLine = contextLine;
  }

  public  Boolean isInApp() {
    return inApp;
  }

  public void setInApp(final  Boolean inApp) {
    this.inApp = inApp;
  }

  public  String getPackage() {
    return _package;
  }

  public void setPackage(final  String _package) {
    this._package = _package;
  }

  public  String getPlatform() {
    return platform;
  }

  public void setPlatform(final  String platform) {
    this.platform = platform;
  }

  public  String getImageAddr() {
    return imageAddr;
  }

  public void setImageAddr(final  String imageAddr) {
    this.imageAddr = imageAddr;
  }

  public  String getSymbolAddr() {
    return symbolAddr;
  }

  public void setSymbolAddr(final  String symbolAddr) {
    this.symbolAddr = symbolAddr;
  }

  public  String getInstructionAddr() {
    return instructionAddr;
  }

  public void setInstructionAddr(final  String instructionAddr) {
    this.instructionAddr = instructionAddr;
  }

  public  Boolean isNative() {
    return _native;
  }

  public void setNative(final  Boolean _native) {
    this._native = _native;
  }

  public  String getRawFunction() {
    return rawFunction;
  }

  public void setRawFunction(final  String rawFunction) {
    this.rawFunction = rawFunction;
  }

  
  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(final  String symbol) {
    this.symbol = symbol;
  }

  
  public LockReason getLock() {
    return lock;
  }

  public void setLock(final  LockReason lock) {
    this.lock = lock;
  }

  // region json

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
    if (colno != null) {
      values.put(JsonKeys.COLNO, colno);
    }
    if (absPath != null) {
      values.put(JsonKeys.ABS_PATH, absPath);
    }
    if (contextLine != null) {
      values.put(JsonKeys.CONTEXT_LINE, contextLine);
    }
    if (inApp != null) {
      values.put(JsonKeys.IN_APP, inApp);
    }
    if (_package != null) {
      values.put(JsonKeys.PACKAGE, _package);
    }
    if (_native != null) {
      values.put(JsonKeys.NATIVE, _native);
    }
    if (platform != null) {
      values.put(JsonKeys.PLATFORM, platform);
    }
    if (imageAddr != null) {
      values.put(JsonKeys.IMAGE_ADDR, imageAddr);
    }
    if (symbolAddr != null) {
      values.put(JsonKeys.SYMBOL_ADDR, symbolAddr);
    }
    if (instructionAddr != null) {
      values.put(JsonKeys.INSTRUCTION_ADDR, instructionAddr);
    }
    if (rawFunction != null) {
      values.put(JsonKeys.RAW_FUNCTION, rawFunction);
    }
    if (symbol != null) {
      values.put(JsonKeys.SYMBOL, symbol);
    }
    if (lock != null) {
      values.put(JsonKeys.LOCK, lock);
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
    public static final String FILENAME = "filename";
    public static final String FUNCTION = "function";
    public static final String MODULE = "module";
    public static final String LINENO = "lineno";
    public static final String COLNO = "colno";
    public static final String ABS_PATH = "abs_path";
    public static final String CONTEXT_LINE = "context_line";
    public static final String IN_APP = "in_app";
    public static final String PACKAGE = "package";
    public static final String NATIVE = "native";
    public static final String PLATFORM = "platform";
    public static final String IMAGE_ADDR = "image_addr";
    public static final String SYMBOL_ADDR = "symbol_addr";
    public static final String INSTRUCTION_ADDR = "instruction_addr";
    public static final String RAW_FUNCTION = "raw_function";
    public static final String SYMBOL = "symbol";
    public static final String LOCK = "lock";
  }
}
