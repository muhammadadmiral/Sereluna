package com.android.capstone.sereluna.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object DarkModePrefUtil {
    private const val PREFERENCE_NAME = "dark_mode_pref"
    private const val KEY_THEME_MODE = "theme_mode" // 0: Default, 1: Light, 2: Dark

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, 0)
    }

    fun setThemeMode(context: Context, mode: Int) {
        getPreferences(context).edit().putInt(KEY_THEME_MODE, mode).apply()
        applyTheme(mode)
    }

    fun applySavedMode(context: Context) {
        val mode = getThemeMode(context)
        applyTheme(mode)
    }

    private fun applyTheme(mode: Int) {
        val appCompatMode = when (mode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(appCompatMode)
    }

    // Keep for compatibility if needed, but preferred to use getThemeMode
    fun isDarkMode(context: Context): Boolean {
        val mode = getThemeMode(context)
        if (mode == 2) return true
        if (mode == 1) return false
        return isSystemDarkMode(context)
    }

    private fun isSystemDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
