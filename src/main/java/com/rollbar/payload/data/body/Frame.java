package com.rollbar.payload.data.body;

import com.google.gson.annotations.SerializedName;
import com.rollbar.payload.utilities.ArgumentNullException;
import com.rollbar.payload.utilities.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Frame {
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

    public Frame(String filename) throws ArgumentNullException {
        this(filename, null, null, null, null, null, null, null);
    }

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

    public String filename() {
        return filename;
    }

    public Frame filename(String filename) throws ArgumentNullException {
        return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
    }

    public Integer lineNumber() {
        return lineNumber;
    }

    public Frame lineNumber(Integer lineNumber) {
        try {
            return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("filename cannot be null");
        }
    }

    public Integer columnNumber() {
        return columnNumber;
    }

    public Frame columnNumber(Integer columnNumber) {
        try {
            return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("filename cannot be null");
        }
    }

    public String method() {
        return method;
    }

    public Frame method(String method) {
        try {
            return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("filename cannot be null");
        }
    }

    public String code() {
        return code;
    }

    public Frame code(String code) {
        try {
            return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("filename cannot be null");
        }
    }

    public CodeContext context() {
        return context;
    }

    public Frame context(CodeContext context) {
        try {
            return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("filename cannot be null");
        }
    }

    public Object[] args() {
        return args == null ? null : args.clone();
    }

    public Frame args(Object[] args) {
        try {
            return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("filename cannot be null");
        }
    }

    public HashMap<String, Object> keywordArgs() {
        return keywordArgs == null ? null : new HashMap<String, Object>(keywordArgs);
    }

    public Frame keywordArgs(HashMap<String, Object> keywordArgs) {
        try {
            return new Frame(filename, lineNumber, columnNumber, method, code, context, args, keywordArgs);
        } catch (ArgumentNullException e) {
            throw new IllegalStateException("filename cannot be null");
        }
    }
}
