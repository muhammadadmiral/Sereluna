package com.android.capstone.sereluna.data.repository

import android.net.Uri
import com.android.capstone.sereluna.data.api.ChatFinishRequestDto
import com.android.capstone.sereluna.data.api.ChatRequestDto
import com.android.capstone.sereluna.data.api.ChatResponseDto
import com.android.capstone.sereluna.data.api.DeviceTokenRequestDto
import com.android.capstone.sereluna.data.api.DiaryDetailDto
import com.android.capstone.sereluna.data.api.DiaryItemDto
import com.android.capstone.sereluna.data.api.DiaryEntryItemDto
import com.android.capstone.sereluna.data.api.DiaryMessagesResponseDto
import com.android.capstone.sereluna.data.api.CalendarDetailDto
import com.android.capstone.sereluna.data.api.CalendarSleepDetailDto
import com.android.capstone.sereluna.data.api.CalendarSummaryItemDto
import com.android.capstone.sereluna.data.api.CalendarWellbeingDto
import com.android.capstone.sereluna.data.api.MoodRequestDto
import com.android.capstone.sereluna.data.api.NotificationItemDto
import com.android.capstone.sereluna.data.api.ScreeningRequestDto
import com.android.capstone.sereluna.data.api.ScreeningResponseDto
import com.android.capstone.sereluna.data.api.SerelunaApi
import com.android.capstone.sereluna.data.api.SerelunaApiClient
import com.android.capstone.sereluna.data.api.SleepDailyItemDto
import com.android.capstone.sereluna.data.api.SleepDailyRequestDto
import com.android.capstone.sereluna.data.api.SuccessResponseDto
import com.android.capstone.sereluna.data.api.UserContextResponseDto
import com.android.capstone.sereluna.data.api.UserProfileResponseDto
import com.android.capstone.sereluna.data.api.UserProfileUpdateRequestDto
import com.android.capstone.sereluna.data.api.ForgotPasswordRequestDto
import com.android.capstone.sereluna.data.api.ForgotPasswordResponseDto
import com.android.capstone.sereluna.data.api.ChangePasswordRequestDto
import com.android.capstone.sereluna.data.api.MessageResponseDto
import com.android.capstone.sereluna.data.api.MoodDistributionResponseDto
import com.android.capstone.sereluna.data.api.SleepTrendsResponseDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SerelunaRepository(
    private val api: SerelunaApi = SerelunaApiClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private suspend fun authHeader(): String {
        val user = auth.currentUser ?: error("User belum login")
        val token = user.getIdToken(false).await().token ?: error("Firebase ID token kosong")
        return "Bearer $token"
    }

    suspend fun sendChat(
        text: String,
        roomId: String?,
        sessionId: String?,
        moodSignal: String?
    ): ChatResponseDto {
        return api.chat(
            authorization = authHeader(),
            request = ChatRequestDto(
                text = text,
                room_id = roomId,
                session_id = sessionId,
                mood_signal = moodSignal,
                mode = "chat"
            )
        )
    }

    suspend fun finishChat(roomId: String, sessionId: String): ChatResponseDto {
        return api.finishChat(
            authorization = authHeader(),
            request = ChatFinishRequestDto(
                room_id = roomId,
                session_id = sessionId
            )
        )
    }

    suspend fun submitScreening(answers: List<Int>, note: String?): ScreeningResponseDto {
        return api.submitScreening(
            authorization = authHeader(),
            request = ScreeningRequestDto(
                answers = answers,
                note = note
            )
        )
    }

    suspend fun getContext(): UserContextResponseDto {
        return api.getContext(authHeader())
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getProfile(): UserProfileResponseDto {
        return api.getProfile(authHeader())
    }

    suspend fun getProfileAsMap(): Map<String, Any> {
        val profile = getProfile()
        return mapOf(
            "uid" to profile.uid,
            "name" to profile.name,
            "email" to profile.email,
            "photoUrl" to profile.photo_url,
            "photo_url" to profile.photo_url,
            "provider" to profile.provider,
            "latestScreeningSummary" to profile.latest_screening_summary,
            "latestDiarySummary" to profile.latest_diary_summary,
            "personalContext" to profile.personal_context,
            "hasScreeningToday" to profile.has_screening_today
        )
    }

    suspend fun updateProfile(name: String, newImageUri: Uri?): UserProfileResponseDto {
        val photoUrl = if (newImageUri != null) uploadProfileImage(newImageUri) else null
        return api.updateProfile(
            authorization = authHeader(),
            request = UserProfileUpdateRequestDto(
                name = name,
                photo_url = photoUrl
            )
        )
    }

    suspend fun getDiaries(limit: Int = 30): List<DiaryItemDto> {
        return api.getDiaries(authHeader(), limit).items
    }

    suspend fun getDiaryEntries(limit: Int = 30): List<DiaryEntryItemDto> {
        return api.getDiaryEntries(authHeader(), limit).items
    }

    suspend fun getDiaryDetail(diaryId: String): DiaryDetailDto {
        return api.getDiaryDetail(authHeader(), diaryId)
    }

    suspend fun getDiaryMessages(diaryId: String, sessionId: String): DiaryMessagesResponseDto {
        return api.getDiaryMessages(authHeader(), diaryId, sessionId)
    }

    suspend fun getNotifications(limit: Int = 30): List<NotificationItemDto> {
        return api.getNotifications(authHeader(), limit).items
    }

    suspend fun markNotificationRead(notificationId: String): SuccessResponseDto {
        return api.markNotificationRead(authHeader(), notificationId)
    }

    suspend fun markAllNotificationsRead(): SuccessResponseDto {
        return api.markAllNotificationsRead(authHeader())
    }

    suspend fun submitDeviceToken(token: String): SuccessResponseDto {
        return api.submitDeviceToken(
            authorization = authHeader(),
            request = DeviceTokenRequestDto(token)
        )
    }

    suspend fun forgotPassword(email: String, continueUrl: String? = null): ForgotPasswordResponseDto {
        return api.forgotPassword(ForgotPasswordRequestDto(email, continueUrl))
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): MessageResponseDto {
        return api.changePassword(
            authorization = authHeader(),
            request = ChangePasswordRequestDto(oldPassword, newPassword)
        )
    }

    suspend fun deleteAccount(): MessageResponseDto {
        return api.deleteAccount(authHeader())
    }

    suspend fun getMoodDistribution(days: Int): MoodDistributionResponseDto {
        return api.getMoodDistribution(authHeader(), days)
    }

    suspend fun getSleepTrends(days: Int): SleepTrendsResponseDto {
        return api.getSleepTrends(authHeader(), days)
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun submitSleepDaily(
        date: String,
        bedtime: String,
        wakeup: String,
        totalSleepHours: Double,
        sleepQuality: String,
    ) {
        api.submitSleepDaily(
            authorization = authHeader(),
            request = SleepDailyRequestDto(
                date = date,
                bedtime = bedtime,
                wakeup = wakeup,
                total_sleep_hours = totalSleepHours,
                sleep_quality = sleepQuality
            )
        )
    }

    suspend fun getSleepDaily(limit: Int = 14): List<SleepDailyItemDto> {
        return api.getSleepDaily(authHeader(), limit).items
    }

    suspend fun submitMood(date: String, mood: String): SuccessResponseDto {
        return api.submitMood(
            authorization = authHeader(),
            request = MoodRequestDto(
                date = date,
                mood = mood
            )
        )
    }

    suspend fun getCalendarSummary(year: Int, month: Int): List<CalendarSummaryItemDto> {
        val raw = api.getCalendarSummary(authHeader(), year, month)
        return raw.itemArray().mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            CalendarSummaryItemDto(
                date = item.stringOrNull("date").orEmpty(),
                has_sleep_data = item.booleanOrFalse("has_sleep_data"),
                mood = item.stringOrNull("mood"),
                has_diary = item.booleanOrFalse("has_diary"),
                wellbeing_score = item.intOrNull("wellbeing_score"),
                wellbeing_level = item.stringOrNull("wellbeing_level"),
                indicator = item.stringOrNull("indicator")
            )
        }.filter { it.date.isNotBlank() }
    }

    suspend fun getCalendarDetail(date: String): CalendarDetailDto {
        val raw = api.getCalendarDetail(authHeader(), date)
        val root = raw.asObjectOrNull() ?: return CalendarDetailDto(date = date)
        val sleep = root.get("sleep").asObjectOrNull()
        val wellbeing = root.get("wellbeing").asObjectOrNull()
        val diarySnippet = root.stringOrNull("diary_snippet")
            ?: root.stringOrNull("diary_summary")
            ?: root.stringOrNull("preview")

        return CalendarDetailDto(
            date = root.stringOrNull("date") ?: date,
            mood = root.stringOrNull("mood"),
            has_sleep_data = root.booleanOrFalse("has_sleep_data") || sleep != null,
            has_diary = root.booleanOrFalse("has_diary") || !diarySnippet.isNullOrBlank(),
            diary_snippet = diarySnippet,
            sleep = CalendarSleepDetailDto(
                bedtime = sleep?.stringOrNull("bedtime") ?: root.stringOrNull("bedtime"),
                wakeup = sleep?.stringOrNull("wakeup") ?: root.stringOrNull("wakeup"),
                total_sleep_hours = sleep?.doubleOrNull("total_sleep_hours")
                    ?: root.doubleOrNull("total_sleep_hours"),
                sleep_quality = sleep?.stringOrNull("sleep_quality")
                    ?: root.stringOrNull("sleep_quality")
            ),
            wellbeing = CalendarWellbeingDto(
                score = wellbeing?.intOrNull("score") ?: root.intOrNull("wellbeing_score"),
                level = wellbeing?.stringOrNull("level") ?: root.stringOrNull("wellbeing_level"),
                signals = wellbeing?.stringList("signals") ?: root.stringList("signals"),
                recommendation = wellbeing?.stringOrNull("recommendation")
                    ?: root.stringOrNull("recommendation")
            )
        )
    }

    private suspend fun uploadProfileImage(imageUri: Uri): String {
        val userId = getCurrentUserId() ?: error("User belum login")
        val storageRef = storage.reference.child("profile_images/$userId")
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    private fun JsonElement.itemArray(): List<JsonElement> {
        if (isJsonArray) return asJsonArray.map { it }
        val root = asObjectOrNull() ?: return emptyList()
        val candidates = listOf("items", "days", "summary", "data")
        val array = candidates.asSequence()
            .mapNotNull { key -> root.get(key)?.takeIf { it.isJsonArray }?.asJsonArray }
            .firstOrNull()
            ?: return emptyList()
        return array.map { it }
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? {
        return if (this != null && !isJsonNull && isJsonObject) asJsonObject else null
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        return runCatching { get(key)?.takeIf { !it.isJsonNull }?.asString }.getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.booleanOrFalse(key: String): Boolean {
        return runCatching { get(key)?.takeIf { !it.isJsonNull }?.asBoolean }.getOrNull() ?: false
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        return runCatching { get(key)?.takeIf { !it.isJsonNull }?.asInt }.getOrNull()
    }

    private fun JsonObject.doubleOrNull(key: String): Double? {
        return runCatching { get(key)?.takeIf { !it.isJsonNull }?.asDouble }.getOrNull()
    }

    private fun JsonObject.stringList(key: String): List<String> {
        val array = runCatching { get(key)?.takeIf { it.isJsonArray }?.asJsonArray }.getOrNull()
            ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { element.takeIf { !it.isJsonNull }?.asString }.getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
    }

    companion object {
        fun todayString(date: Date = Date()): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }
}
