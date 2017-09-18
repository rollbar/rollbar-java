package com.rollbar.payload.data.body;

import com.rollbar.utilities.ArgumentNullException;
import com.rollbar.utilities.JsonSerializable;
import com.rollbar.utilities.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single frame from a stack trace
 */
public class Frame implements JsonSerializable {
    private final String filename;
    private final Integer lineNumber;
    private final Integer columnNumber;
    private final String method;
    private final String code;
    private final String className;
    private final CodeContext context;
    private final Object[] args;
    private final LinkedHashMap<String, Object> keywordArgs;

    /**
     * Constructor
     * @param filename the name of the file in which the error occurred
     */
    public Frame(String filename) {
        this(filename, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor
     * @param filename the name of the file in which the error occurred
     * @param lineNumber the line number on which the error occurred
     * @param columnNumber the column number (if available in your language) on which the error occurred
     * @param method the method in which the error occurred
     * @param code the line of code that triggered the error
     * @param context extra context around the line of code that triggered the error
     * @param args the arguments to the method from the stack frame (if available in your language)
     * @param keywordArgs the keyword arguments to the method from the stack frame (if available in your language)
     */
    public Frame(String filename, Integer lineNumber, Integer columnNumber, String method, String code, CodeContext context, Object[] args, Map<String, Object> keywordArgs) {
        this(filename, lineNumber, columnNumber, method, code, null, context, args, keywordArgs);
    }

    /**
     * Constructor
     * @param filename the name of the file in which the error occurred
     * @param lineNumber the line number on which the error occurred
     * @param columnNumber the column number (if available in your language) on which the error occurred
     * @param method the method in which the error occurred
     * @param code the line of code that triggered the error
     * @param className the name of the class in which the error occurred
     * @param context extra context around the line of code that triggered the error
     * @param args the arguments to the method from the stack frame (if available in your language)
     * @param keywordArgs the keyword arguments to the method from the stack frame (if available in your language)
     */
    public Frame(String filename, Integer lineNumber, Integer columnNumber, String method, String code, String className, CodeContext context, Object[] args, Map<String, Object> keywordArgs) {
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.method = method;
        this.code = code;
        this.className = className;
        this.context = context;
        this.args = args == null ? null : args.clone();
        this.keywordArgs = keywordArgs == null ? null : new LinkedHashMap<String, Object>(keywordArgs);
    }

    /**
     * Get an array of frames from an error
     * @param error the error
     * @return the frames representing the error's stack trace
     * @throws ArgumentNullException if error is null
     */
    public static Frame[] fromThrowable(Throwable error) throws ArgumentNullException {
        Validate.isNotNull(error, "error");
        StackTraceElement[] elements = error.getStackTrace();
        ArrayList<Frame> result = new ArrayList<Frame>();
        for(StackTraceElement element : elements) {
            result.add(fromStackTraceElement(element));
        }
        Collections.reverse(result);
        return result.toArray(new Frame[result.size()]);
    }

    /**
     * Get a Frame from a StackTraceElement
     * @param stackTraceElement the StackTraceElement (a.k.a.: stack frame)
     * @return the Frame representing the StackTraceElement
     * @throws ArgumentNullException if stackTraceElement is null
     */
    public static Frame fromStackTraceElement(StackTraceElement stackTraceElement) throws ArgumentNullException {
        String filename = stackTraceElement.getFileName();
        Integer lineNumber = stackTraceElement.getLineNumber();
        String method = stackTraceElement.getMethodName();
        String className = stackTraceElement.getClassName();

        return new Frame(filename, lineNumber, null, method, null, className, null, null, null);
    }

    /**
    * @return the name of the file in which the error occurred
    */
    public String filename() {
        return filename;
    }

    /**
    * Set filename on a copy of this Frame
    * @param filename the name of the file in which the error occurred
    * @return a copy of this Frame with the filename overridden
    */
    public Frame filename(String filename) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return the line number on which the error occurred
    */
    public Integer lineNumber() {
        return lineNumber;
    }

    /**
    * Set lineNumber on a copy of this Frame
    * @param lineNumber the line number on which the error occurred
    * @return a copy of this Frame with the lineNumber overridden
    */
    public Frame lineNumber(Integer lineNumber) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return the column number (if available in your language) on which the error occurred
    */
    public Integer columnNumber() {
        return columnNumber;
    }

    /**
    * Set columnNumber on a copy of this Frame
    * @param columnNumber the column number (if available in your language) on which the error occurred
    * @return a copy of this Frame with the columnNumber overridden
    */
    public Frame columnNumber(Integer columnNumber) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return the method in which the error occurred
    */
    public String method() {
        return method;
    }

    /**
    * Set method on a copy of this Frame
    * @param method the method in which the error occurred
    * @return a copy of this Frame with the method overridden
    */
    public Frame method(String method) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return the line of code that triggered the error
    */
    public String code() {
        return code;
    }

    /**
    * Set code on a copy of this Frame
    * @param code the line of code that triggered the error
    * @return a copy of this Frame with the code overridden
    */
    public Frame code(String code) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return the name of the class in which the error occurred
    */
    public String className() {
        return className;
    }

    /**
    * Set className on a copy of this Frame
    * @param className the name of the class in which the error occurred
    * @return a copy of this Frame with the className overridden
    */
    public Frame className(String className) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return extra context around the line of code that triggered the error
    */
    public CodeContext context() {
        return context;
    }

    /**
    * Set context on a copy of this Frame
    * @param context extra context around the line of code that triggered the error
    * @return a copy of this Frame with the context overridden
    */
    public Frame context(CodeContext context) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return the arguments to the method from the stack frame (if available in your language)
    */
    public Object[] args() {
        return args == null ? null : args.clone();
    }

    /**
    * Set args on a copy of this Frame
    * @param args the arguments to the method from the stack frame (if available in your language)
    * @return a copy of this Frame with the args overridden
    */
    public Frame args(Object[] args) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    /**
    * @return the keyword arguments to the method from the stack frame (if available in your language)
    */
    public Map<String, Object> keywordArgs() {
        return keywordArgs == null ? null : new LinkedHashMap<String, Object>(keywordArgs);
    }

    /**
    * Set keywordArgs on a copy of this Frame
    * @param keywordArgs the keyword arguments to the method from the stack frame (if available in your language)
    * @return a copy of this Frame with the keywordArgs overridden
    */
    public Frame keywordArgs(Map<String, Object> keywordArgs) {
        return new Frame(filename, lineNumber, columnNumber, method, code, className, context, args, keywordArgs);
    }

    public Map<String, Object> asJson() {

        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        String filename = filename();
        Integer lineNumber = lineNumber();
        Integer columnNumber = columnNumber();
        String method = method();
        String code = code();
        String className = className();
        CodeContext context = context();
        Object[] args = args();
        Map<String, Object> keywordArgs = keywordArgs();

        obj.put("filename", filename != null ? filename : "[unknown]");
        if (lineNumber != null) {
            obj.put("lineno", lineNumber);
        }
        if (columnNumber != null) {
            obj.put("colno", columnNumber);
        }
        if (method != null) {
            obj.put("method", method);
        }
        if (code != null) {
            obj.put("code", code);
        }
        if (className != null) {
            obj.put("class_name", className);
        }
        if (context != null) {
            obj.put("context", context);
        }
        if (args != null) {
            obj.put("args", args);
        }
        if (keywordArgs != null) {
            obj.put("kwargs", keywordArgs);
        }

        return obj;
    }
}
