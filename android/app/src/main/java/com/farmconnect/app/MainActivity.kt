package com.farmconnect.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.farmconnect.app.data.local.LanguagePreferences
import com.farmconnect.app.data.local.ThemePreferences
import com.farmconnect.app.ui.navigation.FarmConnectNavigation
import com.farmconnect.app.ui.theme.FarmConnectTheme
import com.farmconnect.app.utils.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        // Apply saved language using SharedPreferences (safe, works before Hilt)
        val prefs = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("language", "system") ?: "system"
        val context = LocaleHelper.setLocale(newBase, savedLanguage)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            // Get theme preference
            val themePreferences = remember { ThemePreferences(applicationContext) }
            val currentTheme by themePreferences.theme.collectAsState(initial = ThemePreferences.THEME_SYSTEM)
            val systemInDarkTheme = isSystemInDarkTheme()
            
            // Determine if dark theme should be used
            val darkTheme = when (currentTheme) {
                ThemePreferences.THEME_DARK -> true
                ThemePreferences.THEME_LIGHT -> false
                else -> systemInDarkTheme // THEME_SYSTEM
            }
            
            FarmConnectTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FarmConnectNavigation()
                }
            }
        }
    }
}
