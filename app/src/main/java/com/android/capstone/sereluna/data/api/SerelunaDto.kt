package com.android.capstone.sereluna.data.api

data class ChatRequestDto(
    val text: String,
    val room_id: String? = null,
    val session_id: String? = null,
    val mood_signal: String? = "",
    val mode: String? = "chat"
)

data class ChatFinishRequestDto(
    val room_id: String,
    val session_id: String
)

data class ChatResponseDto(
    val reply: String = "",
    val ui_metadata: UiMetadataDto = UiMetadataDto(),
    val clinical_insight: ClinicalInsightDto = ClinicalInsightDto(),
    val session_summary: String = "",
    val room_id: String? = null,
    val session_id: String? = null,
    val algorithm_trace: Map<String, Any>? = null
)

data class UiMetadataDto(
    val sentiment_score: Int = 0,
    val suggested_action: String? = null,
    val is_risky: Boolean = false
)

data class ClinicalInsightDto(
    val detected_symptoms: List<String> = emptyList(),
    val dass_category: String = "None",
    val risk_level: String = "low"
)

data class ScreeningRequestDto(
    val answers: List<Int>,
    val note: String? = ""
)

data class ScreeningResponseDto(
    val date: String = "",
    val scores: Map<String, Int> = emptyMap(),
    val severity: Map<String, String> = emptyMap(),
    val summary: String = "",
    val has_screening_today: Boolean = false,
    val algorithm: Map<String, Any>? = null
)

data class UserContextResponseDto(
    val profile_context: String = "",
    val latest_screening_summary: String = "",
    val latest_diary_summary: String = "",
    val past_diaries: List<String> = emptyList(),
    val has_screening_today: Boolean = false
)

data class UserProfileResponseDto(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photo_url: String = "",
    val provider: String = "",
    val latest_screening_summary: String = "",
    val latest_diary_summary: String = "",
    val personal_context: String = "",
    val has_screening_today: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class UserProfileUpdateRequestDto(
    val name: String,
    val photo_url: String? = null
)

data class DiaryListResponseDto(
    val items: List<DiaryItemDto> = emptyList()
)

data class DiaryItemDto(
    val id: String = "",
    val date: String = "",
    val chat_summary: String = "",
    val created_at: String? = null,
    val updated_at: String? = null
)

data class DiaryEntryListResponseDto(
    val items: List<DiaryEntryItemDto> = emptyList()
)

data class DiaryEntryItemDto(
    val id: String = "",
    val diary_id: String = "",
    val session_id: String = "",
    val date: String = "",
    val summary: String = "",
    val preview: String = "",
    val status: String = "",
    val model: String = "",
    val start_time: String? = null,
    val end_time: String? = null,
    val updated_at: String? = null
)

data class DiaryDetailDto(
    val id: String = "",
    val date: String = "",
    val chat_summary: String = "",
    val sessions: List<DiarySessionDto> = emptyList()
)

data class DiarySessionDto(
    val id: String = "",
    val model: String = "",
    val summary: String = "",
    val start_time: String? = null,
    val end_time: String? = null
)

data class DiaryMessagesResponseDto(
    val items: List<DiaryMessageDto> = emptyList()
)

data class DiaryMessageDto(
    val id: String = "",
    val sender_role: String = "",
    val text: String = "",
    val timestamp: String? = null
)

data class NotificationsResponseDto(
    val items: List<NotificationItemDto> = emptyList()
)

data class NotificationItemDto(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val is_read: Boolean = false,
    val created_at: String? = null
)

data class DeviceTokenRequestDto(
    val token: String
)

data class SleepDailyRequestDto(
    val date: String,
    val sleep_quality: String,
    val total_sleep_hours: Long
)

data class SleepDailyResponseDto(
    val success: Boolean = false
)

data class SleepDailyHistoryResponseDto(
    val items: List<SleepDailyItemDto> = emptyList()
)

data class SleepDailyItemDto(
    val date: String = "",
    val sleep_quality: String = "",
    val total_sleep_hours: Long = 0,
    val updated_at: String? = null
)

data class SuccessResponseDto(
    val success: Boolean = false
)
