package com.android.capstone.sereluna.data.model

import java.util.Date

data class ChatSession(
    var id: String = "",
    var diaryId: String = "",
    var model: String = "prototype",
    var summary: String = "",
    var startedAt: Date? = null,
    var endedAt: Date? = null
)
