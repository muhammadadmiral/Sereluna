package com.android.capstone.sereluna

import android.app.Application
import com.google.firebase.FirebaseApp

class SerelunaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase once for the entire app
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
