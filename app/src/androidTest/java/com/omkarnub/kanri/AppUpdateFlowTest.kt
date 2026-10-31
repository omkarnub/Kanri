package com.omkarnub.kanri

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omkarnub.kanri.data.update.AppUpdateInfo
import com.omkarnub.kanri.data.update.AppUpdateManager
import com.omkarnub.kanri.data.update.AppUpdatePreferences
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUpdateFlowTest {

    @Test
    fun testPostNotificationAndTriggerPopup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val updateManager = AppUpdateManager.getInstance(appContext)
        val updatePrefs = AppUpdatePreferences.getInstance(appContext)

        val sampleUpdate = AppUpdateInfo(
            versionName = "1.0.3",
            releaseTitle = "Kanri v1.0.3 - Multi-Currency & Full-Screen Records",
            releaseNotes = "## What's Changed in Kanri v1.0.3\n\n- Multi-Currency: Expanded support to 52 global currencies with live search\n- Full-Window Records: Redesigned Add Transaction into a dedicated full-screen editor\n- Performance: Streamlined SQLite transactions and reduced frame latency\n- Stability: Background update scheduler and crash-free workers",
            apkDownloadUrl = "https://github.com/omkarnub/Kanri/releases/download/v1.0.2/Kanri-v1.0.2.apk",
            apkSize = 26241398,
            publishedAt = "2026-10-19T14:38:59Z"
        )

        // 1. Post notification
        updateManager.postUpdateAvailableNotification(appContext, sampleUpdate)

        // 2. Store pending update
        updatePrefs.pendingUpdate = sampleUpdate
        assertNotNull(updatePrefs.pendingUpdate)

        // 3. Trigger in-app popup
        updateManager.triggerUpdatePopup()
    }
}
