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
    val session_id: String? = null
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
    val has_screening_today: Boolean = false
)

data class UserContextResponseDto(
    val profile_context: String = "",
    val latest_screening_summary: String = "",
    val latest_diary_summary: String = "",
    val past_diaries: List<String> = emptyList(),
    val has_screening_today: Boolean = false
)
