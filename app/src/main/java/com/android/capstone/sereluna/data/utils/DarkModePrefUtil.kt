package com.android.capstone.sereluna.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object DarkModePrefUtil {
    private const val PREFERENCE_NAME = "dark_mode_pref"
    private const val KEY_DARK_MODE = "is_dark_mode"

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun isDarkMode(context: Context): Boolean =
        getPreferences(context).getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(context: Context, isEnabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_DARK_MODE, isEnabled).apply()
        val mode = if (isEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
