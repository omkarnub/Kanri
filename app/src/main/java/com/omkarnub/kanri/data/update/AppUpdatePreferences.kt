package com.omkarnub.kanri.data.update

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class AppUpdatePreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _pendingUpdateFlow = MutableStateFlow(loadPendingUpdate())
    val pendingUpdateFlow: StateFlow<AppUpdateInfo?> = _pendingUpdateFlow.asStateFlow()

    var lastCheckTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK, value).apply()

    var pendingUpdate: AppUpdateInfo?
        get() = _pendingUpdateFlow.value
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_PENDING_UPDATE_JSON).apply()
            } else {
                prefs.edit().putString(KEY_PENDING_UPDATE_JSON, value.toJson().toString()).apply()
            }
            _pendingUpdateFlow.value = value
        }

    var dismissedVersion: String?
        get() = prefs.getString(KEY_DISMISSED_VERSION, null)
        set(value) = prefs.edit().putString(KEY_DISMISSED_VERSION, value).apply()

    private fun loadPendingUpdate(): AppUpdateInfo? {
        val raw = prefs.getString(KEY_PENDING_UPDATE_JSON, null) ?: return null
        return try {
            AppUpdateInfo.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "kanri_update_prefs"
        private const val KEY_LAST_CHECK = "key_last_check"
        private const val KEY_PENDING_UPDATE_JSON = "key_pending_update_json"
        private const val KEY_DISMISSED_VERSION = "key_dismissed_version"

        @Volatile
        private var INSTANCE: AppUpdatePreferences? = null

        fun getInstance(context: Context): AppUpdatePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppUpdatePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
