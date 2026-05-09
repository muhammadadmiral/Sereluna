package com.android.capstone.sereluna.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ChatRoom(
    @DocumentId
    val roomId: String = "",
    val lastMessage: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val sessionSummary: String = ""
)
