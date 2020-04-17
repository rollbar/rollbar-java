-dontwarn org.slf4j.**

# We need to keep content from these packages.
-keep class com.rollbar.api.** { *; }
-keep class com.rollbar.notifier.sender.** { *; }

# https://www.guardsquare.com/en/products/proguard/manual/examples#serializable
-keepnames class com.rollbar.* implements java.io.Serializable

-keepclassmembers class com.rollbar.* implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepclassmembers,allowoptimization enum com.rollbar.* {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile