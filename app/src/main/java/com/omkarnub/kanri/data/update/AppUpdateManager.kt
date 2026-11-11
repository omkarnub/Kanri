package com.omkarnub.kanri.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.omkarnub.kanri.BuildConfig
import com.omkarnub.kanri.MainActivityDarkDark
import com.omkarnub.kanri.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateManager private constructor(private val context: Context) {

    companion object {
        const val GITHUB_OWNER = "omkarnub"
        const val GITHUB_REPO = "Kanri"
        const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        const val CHANNEL_ID = "kanri_app_updates"
        const val NOTIFICATION_ID = 2024
        const val EXTRA_SHOW_UPDATE_POPUP = "extra_show_update_popup"

        @Volatile
        private var INSTANCE: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppUpdateManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new Kanri app versions"
                    enableVibration(true)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.createNotificationChannel(channel)
            }
        }
    }

    val showUpdatePopupTrigger = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun triggerUpdatePopup() {
        showUpdatePopupTrigger.value = true
    }

    fun dismissUpdatePopupTrigger() {
        showUpdatePopupTrigger.value = false
    }

    /**
     * Secretly queries GitHub Releases API for the latest release and checks
     * if it is newer than [currentVersion].
     */
    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Kanri-Android-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext Result.failure(Exception("GitHub API returned HTTP $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseBody)
            val tagName = json.optString("tag_name", "")
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")
            val assets = json.optJSONArray("assets")

            var apkUrl: String? = null
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrBlank()) {
                return@withContext Result.success(null)
            }

            val cleanRemote = SemanticVersionComparator.cleanVersion(tagName)
            if (SemanticVersionComparator.isNewer(currentVersion, tagName)) {
                val updateInfo = AppUpdateInfo(
                    versionName = cleanRemote,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl,
                    apkSize = apkSize,
                    publishedAt = publishedAt
                )
                Result.success(updateInfo)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Posts the notification as requested:
     * "New version of Kanri is Available. Install now?"
     * which redirects to the update popup when tapped.
     */
    fun postUpdateAvailableNotification(context: Context, updateInfo: AppUpdateInfo) {
        createNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val launchIntent = com.omkarnub.kanri.ui.theme.AppIconManager.createLauncherIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SHOW_UPDATE_POPUP, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Kanri Update Available")
            .setContentText("New version of Kanri is Available. Install now?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("New version of Kanri is Available. Install now?\n\n${updateInfo.releaseTitle}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_widget_add,
                "Install Now",
                pendingIntent
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Gets the downloaded APK file if it already exists and has matching size.
     */
    fun getDownloadedApk(context: Context, updateInfo: AppUpdateInfo): File? {
        val updatesDir = File(context.filesDir, "updates")
        val apkFile = File(updatesDir, "Kanri-${updateInfo.versionName}.apk")
        return if (apkFile.exists() && (updateInfo.apkSize <= 0 || apkFile.length() == updateInfo.apkSize)) {
            apkFile
        } else {
            null
        }
    }

    /**
     * Downloads the APK with redirect resolution and live byte progress updates.
     */
    suspend fun downloadApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.filesDir, "updates").apply { mkdirs() }
            val targetFile = File(updatesDir, "Kanri-${updateInfo.versionName}.apk")
            val tempFile = File(updatesDir, "Kanri-${updateInfo.versionName}.apk.tmp")

            // Clean up any stale files from previous downloads
            updatesDir.listFiles()?.forEach { file ->
                if (file.name != targetFile.name) {
                    file.delete()
                }
            }

            if (targetFile.exists() && (updateInfo.apkSize <= 0 || targetFile.length() == updateInfo.apkSize)) {
                onProgress(1f, targetFile.length(), targetFile.length())
                return@withContext Result.success(targetFile)
            }

            var currentUrl = updateInfo.apkDownloadUrl
            var connection: HttpURLConnection? = null
            var redirects = 0

            // Follow HTTP redirects (GitHub 302 -> AWS S3 asset)
            while (redirects < 6) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 20000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Kanri-Android-App")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location != null) {
                        currentUrl = location
                        redirects++
                        continue
                    }
                }
                connection = conn
                break
            }

            val finalConn = connection ?: return@withContext Result.failure(Exception("Failed to establish download connection"))
            try {
                val finalCode = finalConn.responseCode
                if (finalCode !in 200..299) {
                    tempFile.delete()
                    return@withContext Result.failure(Exception("Failed to download update (HTTP $finalCode)"))
                }
                val totalBytes = if (updateInfo.apkSize > 0) updateInfo.apkSize else finalConn.contentLengthLong

                finalConn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesDownloaded = 0L
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesDownloaded += read
                            val progress = if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            onProgress(progress, bytesDownloaded, totalBytes)
                        }
                    }
                }
            } finally {
                finalConn.disconnect()
            }

            if (tempFile.renameTo(targetFile)) {
                Result.success(targetFile)
            } else {
                tempFile.delete()
                Result.failure(Exception("Failed to finalize downloaded APK file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks unknown sources permission and triggers the Android Package Installer.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> {
        try {
            if (!apkFile.exists()) {
                return Result.failure(Exception("Update file not found"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return Result.failure(SecurityException("UNKNOWN_SOURCES_PERMISSION_REQUIRED"))
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
