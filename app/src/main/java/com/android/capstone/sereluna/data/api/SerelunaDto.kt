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
    val next_recommended_date: String? = null,
    val next_recommended_in_days: Int? = null,
    val recommended_interval_days: Int? = null,
    val updated_at: String? = null,
    val updated_statistics_version: String? = null,
    val disclaimer: String? = null,
    val algorithm: Map<String, Any>? = null
)

data class Dass21QuestionnaireDto(
    val instrument: String = "DASS-21",
    val version: String = "",
    val source_file: String = "",
    val recommended_interval_days: Int = 7,
    val disclaimer: String = "DASS-21 adalah alat screening, bukan diagnosis medis.",
    val instructions: String = "",
    val answer_options: List<Dass21AnswerOptionDto> = emptyList(),
    val questions: List<Dass21QuestionDto> = emptyList()
)

data class Dass21AnswerOptionDto(
    val value: Int = 0,
    val label: String = ""
)

data class Dass21QuestionDto(
    val id: Int = 0,
    val category: String = "",
    val text: String = "",
    val answer_min: Int = 0,
    val answer_max: Int = 3
)

data class ScreeningStatusDto(
    val instrument: String = "DASS-21",
    val recommended_interval_days: Int = 7,
    val is_due: Boolean = true,
    val latest: ScreeningLatestDto? = null,
    val next_recommended_date: String? = null,
    val next_recommended_in_days: Int? = null,
    val server_time: String? = null,
    val updated_at: String? = null,
    val disclaimer: String = "DASS-21 adalah alat screening, bukan diagnosis medis."
)

data class ScreeningLatestDto(
    val date: String = "",
    val severity: Map<String, String> = emptyMap(),
    val scores: Map<String, Int> = emptyMap(),
    val summary: String = ""
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
    val items: List<NotificationItemDto> = emptyList(),
    val unread_count: Int = 0,
    val updated_at: String? = null
)

data class NotificationItemDto(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val priority: String? = null,
    val category_label: String? = null,
    val is_read: Boolean = false,
    val action_link: String? = null,
    val read_at: String? = null,
    val created_at: String? = null
)

data class NotificationUnreadCountDto(
    val unread_count: Int = 0,
    val updated_at: String? = null
)

data class DeviceTokenRequestDto(
    val token: String
)

data class SleepDailyRequestDto(
    val date: String,
    val bedtime: String,
    val wakeup: String,
    val total_sleep_hours: Double,
    val sleep_quality: String
)

data class SleepDailyResponseDto(
    val success: Boolean = false
)

data class SleepDailyHistoryResponseDto(
    val items: List<SleepDailyItemDto> = emptyList()
)

data class SleepDailyItemDto(
    val date: String = "",
    val bedtime: String? = null,
    val wakeup: String? = null,
    val sleep_quality: String = "",
    val total_sleep_hours: Double = 0.0,
    val updated_at: String? = null
)

data class MoodRequestDto(
    val date: String,
    val mood: String
)

data class CalendarSummaryItemDto(
    val date: String = "",
    val has_sleep_data: Boolean = false,
    val mood: String? = null,
    val has_diary: Boolean = false,
    val wellbeing_score: Int? = null,
    val wellbeing_level: String? = null,
    val indicator: String? = null,
    val summary: String? = null,
    val recommendation: String? = null,
    val risk_level: String? = null,
    val model_version: String? = null,
    val screening_context: ScreeningContextDto? = null
)

data class CalendarSleepDetailDto(
    val bedtime: String? = null,
    val wakeup: String? = null,
    val total_sleep_hours: Double? = null,
    val sleep_quality: String? = null
)

data class CalendarWellbeingDto(
    val score: Int? = null,
    val level: String? = null,
    val signals: List<String> = emptyList(),
    val recommendation: String? = null,
    val summary: String? = null,
    val indicator: String? = null,
    val risk_level: String? = null
)

data class ScreeningContextDto(
    val latest_date: String? = null,
    val stress: String? = null,
    val anxiety: String? = null,
    val depression: String? = null,
    val disclaimer: String? = null
)

data class CalendarDetailDto(
    val date: String = "",
    val mood: String? = null,
    val has_sleep_data: Boolean = false,
    val has_diary: Boolean = false,
    val diary_snippet: String? = null,
    val sleep: CalendarSleepDetailDto = CalendarSleepDetailDto(),
    val wellbeing: CalendarWellbeingDto = CalendarWellbeingDto(),
    val summary: String? = null,
    val indicator: String? = null,
    val screening_context: ScreeningContextDto? = null
)

data class SuccessResponseDto(
    val success: Boolean = false,
    val unread_count: Int? = null,
    val updated_at: String? = null
)

data class MessageResponseDto(
    val message: String = ""
)

data class ForgotPasswordRequestDto(
    val email: String,
    val continue_url: String? = null
)

data class ForgotPasswordResponseDto(
    val message: String = "",
    val reset_link: String? = null
)

data class ChangePasswordRequestDto(
    val old_password: String,
    val new_password: String
)

data class MoodDistributionResponseDto(
    val period_days: Int = 0,
    val data: List<MoodCountDto> = emptyList(),
    val dominant_mood: String? = null,
    val insight: String? = null,
    val detail: Map<String, MoodDistributionDetailDto> = emptyMap()
)

data class MoodCountDto(
    val mood: String = "",
    val count: Int = 0
)

data class SleepTrendsResponseDto(
    val average_hours: Double = 0.0,
    val items: List<SleepTrendItemDto> = emptyList(),
    val insight: String? = null
)

data class SleepTrendItemDto(
    val date: String = "",
    val hours: Double = 0.0
)

data class WellbeingStatisticsResponseDto(
    val range: String = "30d",
    val period_days: Int = 30,
    val overall_mood: String = "",
    val average_wellbeing_score: Double? = null,
    val mood_distribution: Map<String, Int> = emptyMap(),
    val mood_distribution_detail: Map<String, MoodDistributionDetailDto> = emptyMap(),
    val dominant_mood: String? = null,
    val screening_context: ScreeningContextDto? = null,
    val insights: List<String> = emptyList(),
    val daily_items: List<WellbeingDailyItemDto> = emptyList(),
    val model_version: String? = null,
    val updated_at: String? = null,
    val updated_statistics_version: String? = null,
    val disclaimer: String? = null
)

data class MoodDistributionDetailDto(
    val label: String = "",
    val count: Int = 0,
    val percent: Double? = null,
    val dates: List<String> = emptyList(),
    val top_signals: List<String> = emptyList(),
    val summary: String? = null,
    val detail_title: String? = null,
    val detail_description: String? = null
)

data class WellbeingDailyItemDto(
    val date: String = "",
    val mood: String? = null,
    val wellbeing_score: Int? = null,
    val wellbeing_level: String? = null,
    val risk_level: String? = null
)

data class ArticleTopicsResponseDto(
    val default_topic: String = "wellbeing",
    val topics: List<ArticleTopicDto> = emptyList(),
    val mood_topic_map: Map<String, String> = emptyMap()
)

data class ArticleTopicDto(
    val key: String = "",
    val label: String = "",
    val summary: String = ""
)

data class ArticleRecommendationsResponseDto(
    val source: String = "The Guardian",
    val query: String = "",
    val topic: String? = null,
    val topic_label: String? = null,
    val topic_summary: String? = null,
    val mood: String? = null,
    val section: String? = null,
    val count: Int = 0,
    val disclaimer: String = "",
    val articles: List<ArticleRecommendationDto> = emptyList()
)

data class ArticleRecommendationDto(
    val id: String = "",
    val title: String = "",
    val section: String? = null,
    val published_at: String? = null,
    val url: String = "",
    val api_url: String? = null,
    val summary: String? = null,
    val thumbnail: String? = null,
    val tags: List<String> = emptyList(),
    val source: String = "The Guardian",
    val topic: String? = null,
    val topic_label: String? = null,
    val content_type: String? = null,
    val content_warning: String? = null,
    val relevance_score: Int? = null,
    val why_recommended: String? = null
)

data class ArticleNotifyRequestDto(
    val article_id: String,
    val title: String,
    val url: String,
    val summary: String? = null
)

data class ArticleNotifyResponseDto(
    val success: Boolean = false,
    val notification_id: String? = null
)

// --- GAMIFICATION DTOs ---

data class GamificationPlayerCardDto(
    val tier_name: String = "Shadow Wanderer",
    val tier_color: String = "#808080",
    val current_xp: Int = 0,
    val next_tier_xp: Int = 100,
    val stardust: Int = 0,
    val streak: Int = 0,
    val eclipse_shields_active: Int = 0,
    val equipped_title: String? = null
)

data class GamificationQuestDto(
    val id: String = "",
    val desc: String = "",
    val progress: Int = 0,
    val target: Int = 1,
    val reward_stardust: Int = 0
)

data class GamificationQuestListDto(
    val daily: List<GamificationQuestDto> = emptyList(),
    val weekly: List<GamificationQuestDto> = emptyList()
)

data class GamificationEclipseResponseDto(
    val success: Boolean = false,
    val message: String = "",
    val shields_active: Int = 0,
    val stardust_remaining: Int = 0
)

data class GamificationOracleResponseDto(
    val reading: String = "",
    val narrative_mood: String = "calm",
    val generated_at: String? = null
)

data class GamificationUpdateDto(
    val xp_gained: Int = 0,
    val stardust_gained: Int = 0,
    val is_tier_up: Boolean = false,
    val celestial_event: Boolean = false,
    val streak_rescued: Boolean = false,
    val oracle_echo: String? = null
)
