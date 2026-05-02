package com.farmconnect.app

import android.content.Context
import android.app.Application
import android.util.Log
import com.farmconnect.app.utils.LocaleHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FarmConnectApp : Application() {
    
    override fun attachBaseContext(base: Context) {
        // Safe language initialization using SharedPreferences (works before Hilt)
        val language = getSavedLanguage(base)
        Log.d("FarmConnectApp", "Loading language: $language")
        val context = LocaleHelper.setLocale(base, language)
        super.attachBaseContext(context)
        Log.d("FarmConnectApp", "Locale applied: ${context.resources.configuration.locales[0]}")
    }
    
    private fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "system") ?: "system"
        Log.d("FarmConnectApp", "Saved language from SharedPrefs: $lang")
        return lang
    }
}
