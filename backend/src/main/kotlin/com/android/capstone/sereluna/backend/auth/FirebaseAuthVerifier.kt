package com.android.capstone.sereluna.backend.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class FirebaseAuthVerifier {
    private val firebaseAuth: FirebaseAuth

    init {
        firebaseAuth = FirebaseAuth.getInstance(initializeApp())
    }

    private fun initializeApp(): FirebaseApp {
        val existing = FirebaseApp.getApps().firstOrNull()
        if (existing != null) {
            return existing
        }

        val serviceAccountPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        val credentials = if (!serviceAccountPath.isNullOrBlank()) {
            FileInputStream(serviceAccountPath).use { serviceAccount ->
                GoogleCredentials.fromStream(serviceAccount)
            }
        } else {
            GoogleCredentials.getApplicationDefault()
        }

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        return FirebaseApp.initializeApp(options)
    }

    suspend fun verifyIdToken(idToken: String): FirebaseToken = withContext(Dispatchers.IO) {
        firebaseAuth.verifyIdToken(idToken)
    }
}
