package com.calistenia.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_preferences")
class PreferencesRepository(private val context: Context) {
    val darkTheme = context.dataStore.data.map { it[DARK_THEME] ?: true }
    val usePounds = context.dataStore.data.map { it[USE_POUNDS] ?: false }
    suspend fun setDarkTheme(value: Boolean) { context.dataStore.edit { it[DARK_THEME] = value } }
    private companion object { val DARK_THEME = booleanPreferencesKey("dark_theme"); val USE_POUNDS = booleanPreferencesKey("use_pounds") }
}
