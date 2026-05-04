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

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# NOTE: -renamesourcefileattribute is a global ProGuard/R8 option and must NOT
# be placed in library consumer rules. If you want to hide original source file
# names in your app's stack traces, add the following to your app's own
# proguard-rules.pro instead:
#   -renamesourcefileattribute SourceFile