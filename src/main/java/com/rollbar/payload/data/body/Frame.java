package com.rollbar.payload.data.body;

import com.google.gson.annotations.SerializedName;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represents a single frame from a stack trace
 */
public class Frame {
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
        String filename = stackTraceElement.getClassName() + ".java";
        Integer lineNumber = stackTraceElement.getLineNumber();
        String method = stackTraceElement.getMethodName();

        return new Frame(filename, lineNumber, null, method, null, null, null, null);
    }

    private final String filename;
    @SerializedName("lineno")
    private final Integer lineNumber;
    @SerializedName("colno")
    private final Integer columnNumber;
    private final String method;
    private final String code;
    private final CodeContext context;
    private final Object[] args;
    @SerializedName("kwargs")
    private final HashMap<String, Object> keywordArgs;

    /**
     * Constructor
     * @param filename the name of the file in which the error occurred
     * @throws ArgumentNullException if filename is null
     */
    public Frame(String filename) throws ArgumentNullException {
        this(filename, null, null, null, null, null, null, null);
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
     * @throws ArgumentNullException if filename is null
     */
    public Frame(String filename, Integer lineNumber, Integer columnNumber, String method, String code, CodeContext context, Object[] args, HashMap<String, Object> keywordArgs) throws ArgumentNullException {
        Validate.isNotNullOrWhitespace(filename, "filename");
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.method = method;
        this.code = code;
        this.context = context;
        this.args = args == null ? null : args.clone();
        this.keywordArgs = keywordArgs == null ? null : new HashMap<String, Object>(keywordArgs);
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
    * @throws ArgumentNullException if filename is null
    */
    public Frame filename(String filename) throws ArgumentNullException {
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
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
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
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
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
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
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
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
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
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
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
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
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
    }

    /**
    * @return the keyword arguments to the method from the stack frame (if available in your language)
    */
    public HashMap<String, Object> keywordArgs() {
        return keywordArgs == null ? null : new HashMap<String, Object>(keywordArgs);
    }

    /**
    * Set keywordArgs on a copy of this Frame
    * @param keywordArgs the keyword arguments to the method from the stack frame (if available in your language)
    * @return a copy of this Frame with the keywordArgs overridden
    */
    public Frame keywordArgs(HashMap<String, Object> keywordArgs) {
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
    }
}
