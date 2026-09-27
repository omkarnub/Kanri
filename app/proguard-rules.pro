# ProGuard / R8 Rules for Kanri

# Room Database Entities and DAOs
-keep class com.omkarnub.kanri.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Backup and JSON serialization
-keep class com.omkarnub.kanri.data.backup.** { *; }

# SMS and Notification Parser Models
-keep class com.omkarnub.kanri.data.parser.** { *; }

# Manifest declared components
-keep class com.omkarnub.kanri.KanriApplication { *; }
-keep class com.omkarnub.kanri.MainActivity { *; }
-keep class com.omkarnub.kanri.ui.popup.InstantPopupService { *; }
-keep class com.omkarnub.kanri.ui.popup.InstantPopupReceiver { *; }
-keep class com.omkarnub.kanri.ui.notification.KanriNotificationListenerService { *; }
-keep class com.omkarnub.kanri.widget.** { *; }

# SQLCipher for Android
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class androidx.work.** { *; }

# Security Crypto / Keystore / Google Tink
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn androidx.security.crypto.**
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Kotlin Reflection & Coroutines
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
