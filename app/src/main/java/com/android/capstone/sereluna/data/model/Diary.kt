package com.android.capstone.sereluna.data.model

import java.util.Date

data class Diary(
    var id: String = "",
    var title: String = "",
    var date: String = "",
    var content: String = "",
    var mood: String = "",
    var tags: List<String> = emptyList(),
    var chatSummary: String = "",
    var createdAt: Date? = null,
    var updatedAt: Date? = null
)
