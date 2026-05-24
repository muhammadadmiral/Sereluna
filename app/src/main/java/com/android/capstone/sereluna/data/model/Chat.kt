package com.android.capstone.sereluna.data.model

import android.net.Uri

data class Chat (
    val message: String,
    val senderId: String,
    val isBot: Boolean,
    val status: String? = null,
    val imageUri: Uri? = null,
    val riskLevel: String? = null,
    val suggestedAction: String? = null
)