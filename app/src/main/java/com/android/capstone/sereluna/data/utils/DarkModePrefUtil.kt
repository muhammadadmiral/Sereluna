package com.android.capstone.sereluna.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object DarkModePrefUtil {
    private const val PREFERENCE_NAME = "dark_mode_pref"
    private const val KEY_DARK_MODE = "is_dark_mode"

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun isDarkMode(context: Context): Boolean {
        val prefs = getPreferences(context)
        if (prefs.contains(KEY_DARK_MODE)) {
            return prefs.getBoolean(KEY_DARK_MODE, false)
        }
        return isSystemDarkMode(context)
    }

    fun setDarkMode(context: Context, isEnabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_DARK_MODE, isEnabled).apply()
        val mode = if (isEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applySavedMode(context: Context) {
        val isDarkMode = isDarkMode(context)
        val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun isSystemDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
