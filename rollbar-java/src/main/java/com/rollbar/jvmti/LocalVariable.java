package com.rollbar.jvmti;

public final class LocalVariable {
    final String name;
    final Object value;

    public LocalVariable(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "LocalVariable{"
            + "name='" + name + '\''
            + ", value=" + value
            + '}';
    }
}
