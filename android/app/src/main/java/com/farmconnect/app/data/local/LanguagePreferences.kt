package com.farmconnect.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

@Singleton
class LanguagePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("language")
    }
    
    /**
     * Get the saved language preference
     * Returns: "en", "hi", or "system"
     */
    fun getLanguage(): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "system"
        }
    }
    
    /**
     * Set the language preference
     * @param language "en" (English), "hi" (Hindi), "mr" (Marathi), or "system" (device default)
     */
    suspend fun setLanguage(language: String) {
        android.util.Log.d("LanguagePreferences", "Setting language to: $language")
        context.languageDataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
        // Also save to SharedPreferences for app startup
        setLanguageSync(language)
        android.util.Log.d("LanguagePreferences", "Language saved: $language")
    }
    
    /**
     * Set language synchronously using SharedPreferences
     * Used for immediate app restart and FarmConnectApp initialization
     */
    fun setLanguageSync(language: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language", language).apply()
        android.util.Log.d("LanguagePreferences", "Language saved to SharedPrefs: $language")
    }
}
