package com.android.capstone.sereluna.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object AuthSessionManager {
    private const val PREFS = "sereluna_auth"
    private const val KEY_EXPIRES_AT = "expires_at"

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun startSession(context: Context, rememberMe: Boolean) {
        val duration = if (rememberMe) 30 * DAY_MS else 1 * DAY_MS
        val expiresAt = System.currentTimeMillis() + duration
        prefs(context).edit().putLong(KEY_EXPIRES_AT, expiresAt).apply()
    }

    fun isSessionValid(context: Context): Boolean {
        val expiresAt = prefs(context).getLong(KEY_EXPIRES_AT, 0L)
        return expiresAt > System.currentTimeMillis() && FirebaseAuth.getInstance().currentUser != null
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        FirebaseAuth.getInstance().signOut()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
