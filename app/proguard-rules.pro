# R8 rules for Thai Clock Fix

# Keep enough metadata for de-obfuscating release stack traces with mapping.txt.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Shizuku.newProcess is intentionally called via reflection because the API is
# private/deprecated in Shizuku 13.x. Keep that member until the app migrates to
# Shizuku UserService.
-keepclassmembers class rikka.shizuku.Shizuku {
    private static *** newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}

# The provider is declared in AndroidManifest.xml and is the entry point used by
# Shizuku. Keep it explicit to make the release contract obvious.
-keep class rikka.shizuku.ShizukuProvider { *; }

# Debug/verbose logs are not useful in a public release.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
