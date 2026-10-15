# ==============================================================================
# ProGuard / R8 Rules for Thai Clock Fix
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Stack Trace & Debugging (สำหรับ De-obfuscation / Crash Analysis)
# ------------------------------------------------------------------------------
# เก็บ Line number และ Source file เพื่อให้ stack trace ใน crash report
# สามารถ map กลับไปหาบรรทัดโค้ดจริงผ่าน mapping.txt ได้
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# เก็บ Annotations, Signature (Generics), และ Enclosing methods สำหรับ Kotlin Reflection / Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 2. Shizuku API & IPC / Reflection Protection (สำคัญมาก)
# ------------------------------------------------------------------------------
# Shizuku ทำงานผ่าน Binder / AIDL IPC ข้าม Process
# และ SystemUiLocaleService มีการเรียก Shizuku.newProcess ผ่าน Java Reflection:
#   Shizuku::class.java.getDeclaredMethod("newProcess", ...)
# จึงต้องห้าม R8 ลบหรือเปลี่ยนชื่อ package, class, interface, method ของ Shizuku โดยเด็ดขาด
-keep class rikka.shizuku.** { *; }
-keepinterface rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# ป้องกัน AIDL stubs, Binder proxy และ Provider classes ที่ Shizuku ใช้งาน
-keep class * extends android.os.IInterface { *; }
-keep class * extends android.os.Binder { *; }
-keep class rikka.shizuku.ShizukuProvider { *; }

# ------------------------------------------------------------------------------
# 3. Data Models & Enums
# ------------------------------------------------------------------------------
# ป้องกัน Enums (เช่น LocaleMode, ShizukuStatus, PrimaryAction)
# ไม่ให้ค่า name() หรือ valueOf() ถูก obfuscate จนอ่านไม่ออกหรือกระทบ logic
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ป้องกัน Model และ Service data classes ที่ใช้เก็บผลลัพธ์
-keep class com.example.model.** { *; }
-keep class com.example.service.ShellResult { *; }
-keep class com.example.service.ApplyResult { *; }
-keep class com.example.service.SystemUiSettings { *; }

# ------------------------------------------------------------------------------
# 4. Kotlin Coroutines & Jetpack DataStore / Compose
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**

# ------------------------------------------------------------------------------
# 5. Security & Optimization (ตัด Debug Logs ออกใน Release Build)
# ------------------------------------------------------------------------------
# ลบคำสั่ง Log.d และ Log.v ใน Release build เพื่อความปลอดภัย ไม่ให้ log shell / status รั่วไหล
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
