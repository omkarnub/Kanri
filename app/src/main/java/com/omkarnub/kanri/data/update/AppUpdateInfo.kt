package com.omkarnub.kanri.data.update

import org.json.JSONObject

data class AppUpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSize: Long,
    val publishedAt: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("versionName", versionName)
            put("releaseTitle", releaseTitle)
            put("releaseNotes", releaseNotes)
            put("apkDownloadUrl", apkDownloadUrl)
            put("apkSize", apkSize)
            put("publishedAt", publishedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AppUpdateInfo {
            return AppUpdateInfo(
                versionName = json.optString("versionName", ""),
                releaseTitle = json.optString("releaseTitle", ""),
                releaseNotes = json.optString("releaseNotes", ""),
                apkDownloadUrl = json.optString("apkDownloadUrl", ""),
                apkSize = json.optLong("apkSize", 0L),
                publishedAt = json.optString("publishedAt", "")
            )
        }
    }
}
