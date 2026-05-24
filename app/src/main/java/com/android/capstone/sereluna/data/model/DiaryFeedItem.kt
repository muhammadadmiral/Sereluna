package com.android.capstone.sereluna.data.model

data class DiaryFeedItem(
    val id: String = "",
    val diaryId: String = "",
    val sessionId: String = "",
    val date: String = "",
    val title: String? = null,
    val content: String? = null,
    val summary: String = "",
    val preview: String = "",
    val status: String = "",
    val model: String = "",
    val startTime: String? = null,
    val endTime: String? = null,
    val updatedAt: String? = null
)

data class DiaryDayGroup(
    val date: String,
    val entries: List<DiaryFeedItem>
)
