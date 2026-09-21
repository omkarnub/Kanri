package com.omkarnub.kanri.data.greeting

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.greetingDataStore: DataStore<Preferences> by preferencesDataStore(name = "greeting_preferences")

data class AntiRepetitionState(
    val lastSubtitleRuleId: Int? = null,
    val lastShownEpochDay: Long? = null,
    val lastWarningBucket: String? = null,
    val lastWarningEpochDay: Long? = null
)

class GreetingDataStore(private val context: Context) {

    companion object {
        val KEY_LAST_SUBTITLE_RULE_ID = intPreferencesKey("last_subtitle_rule_id")
        val KEY_LAST_SHOWN_EPOCH_DAY = longPreferencesKey("last_shown_epoch_day")
        val KEY_LAST_WARNING_BUCKET = stringPreferencesKey("last_warning_bucket")
        val KEY_LAST_WARNING_EPOCH_DAY = longPreferencesKey("last_warning_epoch_day")
    }

    val antiRepetitionState: Flow<AntiRepetitionState> = context.greetingDataStore.data.map { prefs ->
        AntiRepetitionState(
            lastSubtitleRuleId = prefs[KEY_LAST_SUBTITLE_RULE_ID],
            lastShownEpochDay = prefs[KEY_LAST_SHOWN_EPOCH_DAY],
            lastWarningBucket = prefs[KEY_LAST_WARNING_BUCKET],
            lastWarningEpochDay = prefs[KEY_LAST_WARNING_EPOCH_DAY]
        )
    }

    suspend fun recordShownRule(
        ruleId: Int,
        epochDay: Long,
        isWarning: Boolean,
        bucket: String
    ) {
        context.greetingDataStore.edit { prefs ->
            prefs[KEY_LAST_SUBTITLE_RULE_ID] = ruleId
            prefs[KEY_LAST_SHOWN_EPOCH_DAY] = epochDay
            if (isWarning) {
                prefs[KEY_LAST_WARNING_BUCKET] = bucket
                prefs[KEY_LAST_WARNING_EPOCH_DAY] = epochDay
            }
        }
    }
}
