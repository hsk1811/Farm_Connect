package com.farmconnect.app.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    
    /**
     * Apply locale to context
     * @param context Application context
     * @param language Language code: "en", "hi", or "system"
     * @return Updated context with new locale
     */
    fun setLocale(context: Context, language: String): Context {
        val locale = when (language) {
            "en" -> Locale("en")
            "hi" -> Locale("hi", "IN")
            "mr" -> Locale("mr", "IN")
            "system" -> getSystemLocale()
            else -> Locale("en")
        }
        
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * Get the device's current locale
     */
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Locale.getDefault(Locale.Category.DISPLAY)
        } else {
            Locale.getDefault()
        }
    }
    
    /**
     * Recreate activity to apply locale change
     */
    fun recreateActivity(activity: Activity) {
        activity.recreate()
    }
    
    /**
     * Context wrapper to override locale
     */
    class LocaleContextWrapper(base: Context) : ContextWrapper(base) {
        companion object {
            fun wrap(context: Context, language: String): ContextWrapper {
                val newContext = setLocale(context, language)
                return LocaleContextWrapper(newContext)
            }
        }
    }
}
