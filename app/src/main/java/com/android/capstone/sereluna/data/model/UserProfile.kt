package com.android.capstone.sereluna.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore schema for documents inside users/{uid}.
 */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val provider: String = "password",
    @ServerTimestamp val createdAt: Timestamp? = null
)
