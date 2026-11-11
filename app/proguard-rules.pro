# ProGuard / R8 Rules for Kanri

# Room Database and legacy annotations
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-dontwarn androidx.room.paging.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Preserve stack trace line numbers & generic signatures without leaking local paths
-keepattributes Signature,Exceptions,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Legacy SQLCipher package warnings if referenced transitively
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Kotlin Reflection & Coroutines
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
