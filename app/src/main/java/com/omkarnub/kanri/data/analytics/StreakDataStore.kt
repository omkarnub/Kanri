package com.omkarnub.kanri.data.analytics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.streakDataStore: DataStore<Preferences> by preferencesDataStore(name = "streak_preferences")

class StreakDataStore(private val context: Context) {

    companion object {
        val KEY_LAST_CELEBRATED_MILESTONE = intPreferencesKey("last_celebrated_milestone")
        val MILESTONES = listOf(3, 7, 14, 30)
    }

    val lastCelebratedMilestone: Flow<Int> = context.streakDataStore.data.map { preferences ->
        preferences[KEY_LAST_CELEBRATED_MILESTONE] ?: 0
    }

    suspend fun setLastCelebratedMilestone(milestone: Int) {
        context.streakDataStore.edit { preferences ->
            preferences[KEY_LAST_CELEBRATED_MILESTONE] = milestone
        }
    }
}
