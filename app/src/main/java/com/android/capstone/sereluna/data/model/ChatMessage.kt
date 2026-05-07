package com.android.capstone.sereluna.data.model

import java.util.Date

data class ChatMessage(
    val role: String = "user", // "user" or "assistant"
    val text: String = "",
    val createdAt: Date? = null
)
