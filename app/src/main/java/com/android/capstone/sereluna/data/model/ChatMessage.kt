package com.android.capstone.sereluna.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ChatMessage(
    @DocumentId
    val messageId: String = "",
    val senderRole: String = "", // "user" or "bot"
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
